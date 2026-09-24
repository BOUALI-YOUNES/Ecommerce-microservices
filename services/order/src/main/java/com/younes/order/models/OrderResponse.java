package com.younes.order.models;

import java.math.BigDecimal;

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
    String customerId;
}
