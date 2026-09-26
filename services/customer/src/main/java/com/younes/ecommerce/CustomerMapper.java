package com.younes.ecommerce;

import org.springframework.stereotype.Service;

/**
 * CustomerMapper
 */
@Service
public class CustomerMapper {

    public Customer toCustomer(CustomerRequest request) {
        return Customer.builder()
            .id(request.getId())
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .email(request.getEmail())
            .address(request.getAddress())
            .build();
    }

}
