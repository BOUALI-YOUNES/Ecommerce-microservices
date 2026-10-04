package com.younes.order.models;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * OrderMapper
 */
@Service
public class OrderMapper {

    /**
     * @param totalAmount computed by the caller from the product catalogue. It is passed
     *                    in rather than read from the request so the client cannot choose
     *                    the price.
     */
    public Order toOrder(OrderRequest orderRequest, String customerId, java.math.BigDecimal totalAmount) {
       return Order.builder()
                            .customerId(customerId)
                            .reference(orderRequest.getReference())
                            .totalAmount(totalAmount)
                            .paymentMethod(orderRequest.getPaymentMethod())
                            .status(OrderStatus.PENDING)
                            .build();
    }

    public OrderResponse fromOrder(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .amount(order.getTotalAmount())
            .paymentMethod(order.getPaymentMethod())
            .status(order.getStatus())
            .reference(order.getReference())
            .orderLines(order.getOrderLines() == null ? List.of()
                    : order.getOrderLines().stream().map(this::fromOrderLine).toList())
            .build();
    }

    private OrderLineResponse fromOrderLine(OrderLine orderLine) {
        return OrderLineResponse.builder()
                .id(orderLine.getId())
                .productId(orderLine.getProductId())
                .quantity(orderLine.getQuantity())
                .build();
    }
}