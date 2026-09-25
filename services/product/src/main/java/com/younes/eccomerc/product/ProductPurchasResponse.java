package com.younes.eccomerc.product;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProductPurchasResponse
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductPurchasResponse {
    Integer productId;
    String name ;
    String description; 
    BigDecimal price;
    double quantity;
}
