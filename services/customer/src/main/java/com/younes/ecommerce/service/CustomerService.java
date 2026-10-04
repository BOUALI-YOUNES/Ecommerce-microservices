package com.younes.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.younes.ecommerce.Customer;
import com.younes.ecommerce.CustomerMapper;
import com.younes.ecommerce.CustomerRequest;
import com.younes.ecommerce.CustomerResponse;
import com.younes.ecommerce.exception.CustomerAccessDeniedException;
import com.younes.ecommerce.exception.CustomerAlreadyExistsException;
import com.younes.ecommerce.exception.CustomerException;
import com.younes.ecommerce.repo.CustomerRepo;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final CustomerMapper customerMapper;

    /**
     * Creates the profile for the authenticated caller.
     *
     * <p>{@code keycloakId} comes from the caller's token, so a profile cannot be
     * created against somebody else's identity, and a second registration for the same
     * account is rejected instead of replacing the existing profile.
     */
    public String createCustomer(CustomerRequest request, String keycloakId) {
        if (customerRepo.existsByKeycloakId(keycloakId)) {
            throw new CustomerAlreadyExistsException(
                    "A customer profile already exists for the authenticated user");
        }
        if (StringUtils.isNotBlank(request.getEmail()) && customerRepo.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException(
                    String.format("A customer with the email %s already exists", request.getEmail()));
        }

        Customer customer = customerRepo.save(customerMapper.toCustomer(request, keycloakId));
        return customer.getId();
    }

    /**
     * Updates the authenticated caller's own profile.
     */
    public void editCustomer(CustomerRequest request, String keycloakId) {
        var customer = customerRepo.findByKeycloakId(keycloakId).orElseThrow(() -> new CustomerException(
            "Cannot update customer :: No profile is associated with the authenticated user"
        ));
        mergeCustomer(customer, request);
        customerRepo.save(customer);
    }

    private void mergeCustomer(Customer customer, CustomerRequest request) {
        if(StringUtils.isNotBlank(request.getFirstname())) {
            customer.setFirstname(request.getFirstname());
        }
        if(StringUtils.isNotBlank(request.getLastname())) {
            customer.setLastname(request.getLastname());
        }
        if(StringUtils.isNotBlank(request.getEmail())) {
            customer.setEmail(request.getEmail());
        }
        if(request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
    }

/**
     * Listing every customer exposes every profile, so it is ADMIN only.
     *
     * <p>The gateway already enforces this, but the service is reachable from every
     * other container on the compose network, so the check is repeated here rather
     * than relying on the edge alone.
     */
    public List<CustomerResponse> getAllCustomers() {
        return customerRepo.findAll()
            .stream().map(this::fromCustomer)
            .collect(Collectors.toList());
    }

    private CustomerResponse fromCustomer(Customer customer) {
        return new CustomerResponse(customer.getId(),customer.getFirstname(), customer.getLastname(), customer.getEmail(), customer.getAddress());
    }

    /**
     * The authenticated caller's own profile. This is what the order service resolves,
     * so an order can never be placed against an arbitrary customer id.
     */
    public CustomerResponse findByKeycloakId(String keycloakId) {
        return customerRepo.findByKeycloakId(keycloakId)
                .map(this::fromCustomer)
                .orElseThrow(() -> new CustomerException(
                    "No customer profile is associated with the authenticated user"
                ));
    }

    /**
     * Reads a single profile. Callers may only read their own; ADMIN may read any.
     *
     * <p>Returning 403 rather than 404 would confirm that the id exists, but the id is
     * an opaque Mongo identifier that a normal user has no way to discover, so the
     * clearer error is kept.
     */
    public CustomerResponse findById(String customerId, String callerKeycloakId, boolean callerIsAdmin) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new CustomerException(
                    String.format("Customer not found with the id %s", customerId)
                ));
        requireOwnerOrAdmin(customer, callerKeycloakId, callerIsAdmin);
        return fromCustomer(customer);
    }

    public boolean existsById(String customerId, String callerKeycloakId, boolean callerIsAdmin) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new CustomerException(
                    String.format("Customer not found with the id %s", customerId)
                ));
        requireOwnerOrAdmin(customer, callerKeycloakId, callerIsAdmin);
        return true;
    }

    /**
     * Deleting a profile is an administrative action, so a caller can never delete
     * their own account by id either.
     */
    public void deleteCustomer(String customerId) {
        if (!customerRepo.existsById(customerId)) {
            throw new CustomerException(String.format("Customer not found with the id %s", customerId));
        }
        customerRepo.deleteById(customerId);
    }

    /**
     * Profiles created before identity binding have no keycloakId. Those are treated
     * as accessible only by ADMIN, never by a caller claiming ownership.
     */
    private void requireOwnerOrAdmin(Customer customer, String callerKeycloakId, boolean callerIsAdmin) {
        if (callerIsAdmin) {
            return;
        }
        if (StringUtils.isBlank(customer.getKeycloakId()) || !customer.getKeycloakId().equals(callerKeycloakId)) {
            throw new CustomerAccessDeniedException(
                    "You are not allowed to access this customer profile");
        }
    }

}