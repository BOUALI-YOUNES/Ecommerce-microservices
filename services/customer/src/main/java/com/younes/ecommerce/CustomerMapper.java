package com.younes.ecommerce;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * CustomerMapper
 */
@Service
@RequiredArgsConstructor
public class CustomerMapper {

    /**
     * The customer's own id is never taken from the request. {@code keycloakId} is
     * supplied by the service from the caller's token.
     */
    public Customer toCustomer(CustomerRequest request, String keycloakId) {
        return Customer.builder()
            .keycloakId(keycloakId)
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .email(request.getEmail())
            .address(request.getAddress())
            .build();
    }

}