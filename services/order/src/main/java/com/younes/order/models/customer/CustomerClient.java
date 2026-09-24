package com.younes.order.models.customer;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * CustomerClient
 */
@FeignClient(
    name = "customer-service",
    url = "${application.conifg.customer-url}"
)
public interface CustomerClient {

    @GetMapping("={customer-id}")
    Optional<CustomerResponse> findCustomerById(@PathVariable("customer-id") String customerId);
}
