package com.younes.order.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * OrderLineResponse
 */
@Getter
@AllArgsConstructor
@Builder
public class OrderLineResponse {
    Integer id;
    Integer productId;
    double quantity;
}