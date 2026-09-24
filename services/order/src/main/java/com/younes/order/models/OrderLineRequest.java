package com.younes.order.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OrderLineRequest
 */
@Getter 
@AllArgsConstructor 
public class OrderLineRequest {
    Integer id;
    Integer orderId;
    Integer productId;
    double quantity;
}
