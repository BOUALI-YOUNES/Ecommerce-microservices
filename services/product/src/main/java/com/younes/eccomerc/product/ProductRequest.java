package com.younes.eccomerc.product;

import java.math.BigDecimal;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * ProductRequest
 */
@Getter
@Setter
public class ProductRequest {

    Integer id;
    @NotNull(message = "The product name is required !")
    String name;
    @NotNull(message = "The product descreption is required !")
    String description;
    @Positive(message = "The product quantity should be positive !")
    double availableQuantity;
    @Positive(message = "The product price should be positive !")
    BigDecimal price;
    @NotNull(message = "The product category is required  !")
    Integer categoryId;
}
