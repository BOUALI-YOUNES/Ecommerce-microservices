package com.younes.order.models.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * CustomerClient
 *
 * Calls the customer service directly through Eureka instead of hair-pinning through
 * the API gateway. The previous URL routed every internal call back through the
 * gateway, which made the gateway a single point of failure for the whole order flow
 * and, because a fixed url disables client-side load balancing, defeated load
 * balancing as well.
 */
@FeignClient(name = "customer-service", path = "/api/v1/customers")
public interface CustomerClient {

    /**
     * Resolves the caller's own customer profile from the forwarded bearer token, so
     * the order is always placed for the authenticated customer rather than a
     * client-supplied id.
     */
    @GetMapping("/me")
    CustomerResponse findCurrentCustomer();
}