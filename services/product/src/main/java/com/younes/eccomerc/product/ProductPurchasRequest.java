package com.younes.eccomerc.product;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * productPurchasRequest
 */
@Getter
public class ProductPurchasRequest {

    @NotNull(message = "Product is required !")
    Integer productId;
    @NotNull(message = "Quantity is required !")
    double quantity;
}
