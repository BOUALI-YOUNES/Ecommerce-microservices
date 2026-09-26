package com.younes.payment.payement;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/**
 * PaymentRequest
 */
@Getter 
public class PaymentRequest {
    private Integer id;
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
    @NotNull(message = "The customer is mandatory !")
    Customer customer;

}
