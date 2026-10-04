package com.younes.order.models;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * The customer id is taken from the caller's token, never from the request body, so
     * an authenticated user cannot place an order against another customer's id.
     */
    @PostMapping
    public ResponseEntity<Integer> createOrder(
            @RequestBody @Valid OrderRequest orderRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.createOrder(orderRequest, jwt.getSubject()));
    }

    /**
     * Scoped to the caller's own orders. Reading somebody else's order requires the
     * ADMIN role, which is enforced at the gateway, so this endpoint is no longer a
     * full dump of every order in the system for any authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.findAllByCustomerId(jwt.getSubject()));
    }

    @GetMapping("/{order-id}")
    public ResponseEntity<OrderResponse> getById(
        @PathVariable("order-id") Integer orderId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(orderService.findByIdAndCustomerId(orderId, jwt.getSubject()));
    }
}