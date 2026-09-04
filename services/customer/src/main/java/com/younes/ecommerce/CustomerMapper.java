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
            .first_name(request.getFirst_name())
            .last_name(request.getLast_name())
            .email(request.getEmail())
            .address(request.getAddress())
            .build();
    }

}
