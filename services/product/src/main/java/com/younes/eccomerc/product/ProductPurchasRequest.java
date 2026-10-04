package com.younes.eccomerc.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/**
 * productPurchasRequest
 */
@Getter
public class ProductPurchasRequest {

    @NotNull(message = "Product is required !")
    Integer productId;
    @Positive(message = "Quantity is required and must be greater than zero !")
    double quantity;
}