package com.younes.order.models;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * OrderResponse
 */
@Getter
@AllArgsConstructor
@Builder
public class OrderResponse {
    Integer id;
    BigDecimal amount;
    String reference;
    PaymentMethod paymentMethod;
    OrderStatus status;
    String customerId;
    List<OrderLineResponse> orderLines;
}