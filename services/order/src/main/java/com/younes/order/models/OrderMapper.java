package com.younes.order.models;

import org.springframework.stereotype.Service;

/**
 * OrderMapper
 */
@Service 
public class OrderMapper {

    public Order toOrder(OrderRequest orderRequest) {
       return Order.builder()
                            .id(orderRequest.getId())
                            .customerId(orderRequest.getCustomerId())
                            .reference(orderRequest.getReference())
                            .totalAmount(orderRequest.getAmount())
                            .paymentMethod(orderRequest.getPaymentMethod())
                            .build();
    }

    public OrderResponse fromOrder(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .amount(order.getTotalAmount())
            .paymentMethod(order.getPaymentMethod())
            .reference(order.getReference())
            .build();
    }
}