package com.younes.order.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * PurchaseRequest
 */
@Getter 
@Setter 
public class PurchaseRequest {

    @NotNull (message = "Product is mandatory")
    Integer productId;
    @Positive (message = "Quantity is mandatory")
    double quantity;

}
