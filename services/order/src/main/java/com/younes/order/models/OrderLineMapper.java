package com.younes.order.models;

import org.springframework.stereotype.Component;

/**
 * OrderLineMapper
 */
@Component
public class OrderLineMapper {

    public OrderLine toOrderLine(OrderLineRequest orderLineRequest) {
       return OrderLine.builder()
                                    .id(orderLineRequest.getId())
                                    .order(Order.builder()
                                                .id(orderLineRequest.getOrderId())
                                                .build()
                                            )
                                    .productId(orderLineRequest.getProductId())
                                    .quantity(orderLineRequest.getQuantity())     
                                    .build();
    }

}
