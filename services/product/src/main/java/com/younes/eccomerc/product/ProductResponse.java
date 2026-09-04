package com.younes.eccomerc.product;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProductResponse
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductResponse {
    Integer Id;
    String name ;
    String description;
    double availableQuantity;
    BigDecimal price;
    Integer categoryId;
    String categoryName;
    String categoryDesciption;
}
