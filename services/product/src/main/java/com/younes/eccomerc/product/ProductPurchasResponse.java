package com.younes.eccomerc.product;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;

/**
 * ProductPurchasResponse
 */
@AllArgsConstructor
public class ProductPurchasResponse {
    Integer productId;
    String name ;
    String description; 
    BigDecimal price;
    double quantity;
}
