package com.younes.payment.payement;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/**
 * PaymentRequest
 *
 * <p>There is deliberately no id field. It was previously mapped onto the entity
 * primary key, so a request carrying an existing id updated that payment row instead
 * of creating a new one, and the order service was forwarding the order's client id
 * as the payment's id.
 */
@Getter
public class PaymentRequest {
    @NotNull(message = "The payment amount is mandatory !")
    @Positive(message = "The payment amount should be positive !")
    private BigDecimal amount;
    @NotNull(message = "The payment method is mandatory !")
    @JsonProperty("paymentMethod")
    private PaymentMethode paymentMethode;
    @NotNull(message = "The order id is mandatory !")
    Integer orderId;
    @NotBlank(message = "The order reference is mandatory !")
    String orderReference;
    @Valid
    @NotNull(message = "The customer is mandatory !")
    Customer customer;

}