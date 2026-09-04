package com.younes.ecommerce.service;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.younes.ecommerce.Customer;
import com.younes.ecommerce.CustomerMapper;
import com.younes.ecommerce.CustomerRequest;
import com.younes.ecommerce.CustomerResponse;
import com.younes.ecommerce.exception.CustomerException;
import com.younes.ecommerce.repo.CustomerRepo;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo; 
    private final CustomerMapper customerMapper;

    

    public String createCustomer(CustomerRequest request) {
        Customer customer = customerRepo.save(customerMapper.toCustomer(request));
        return customer.getId();
    }

    public void editCustomer(CustomerRequest request) {
        var customer = customerRepo.findById(request.getId()).orElseThrow(() -> new CustomerException(
            String.format("Cannot update customer :: No customer found with the id %s", request.getId())
        ));
        mergeCustomer(customer , request);
        customerRepo.save(customer);
    }
     
    private void mergeCustomer(Customer customer, CustomerRequest request) {
        if(StringUtils.isNotBlank(request.getFirst_name())) {
            customer.setFirst_name(request.getFirst_name());
        }
        if(StringUtils.isNotBlank(request.getLast_name())) {
            customer.setLast_name(request.getLast_name());
        }
        if(StringUtils.isNotBlank(request.getEmail())) {
            customer.setEmail(request.getEmail());
        }
        if(request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepo.findAll()
            .stream().map(this::fromCustomer)
            .collect(Collectors.toList());
    }

    private CustomerResponse fromCustomer(Customer customer) {
        return new CustomerResponse(customer.getId(),customer.getFirst_name(), customer.getLast_name(), customer.getEmail(), customer.getAddress());
    }

    public Boolean existsById(String customerId) {
        return customerRepo.existsById(customerId);
    }

    public CustomerResponse findById(String customerId) {
        return customerRepo.findById(customerId)
                .map(this::fromCustomer)
                .orElseThrow(() -> new CustomerException(
                    String.format("Customer not found with the id %s", customerId)
                ));
    }

    public void deleteCustomer(String customerId) {
        customerRepo.deleteById(customerId);
    }

     
}
