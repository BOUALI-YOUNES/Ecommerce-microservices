package com.younes.order.models;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PurchaseResponse
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PurchaseResponse {

    String productId;
    String name ;
    String description;
    BigDecimal price;
    double quantity;
}
