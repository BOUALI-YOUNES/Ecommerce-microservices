package com.younes.order.models;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * OrderRequest
 */
@Data 
public class OrderRequest {
    Integer id;
    String reference;
    @Positive(message = "The order amount should be positive !")
    BigDecimal amount;
    @NotNull(message = "Payement methode is required !")
    PaymentMethod paymentMethod;
    @NotNull (message = "Customer should be present !")
    @NotEmpty  (message = "Customer should be present !")
    @NotBlank (message = "Customer should be present !")
    String customerId;
    @NotEmpty (message = "You have to order one or more products")
    List<PurchaseRequest> products;

}
