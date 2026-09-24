package com.younes.payment.payement;

import java.math.BigDecimal;


import lombok.Getter;

/**
 * PaymentRequest
 */
@Getter 
public class PaymentRequest {
    private Integer id;
    private BigDecimal amount;
    private PaymentMethode paymentMethode;
    Integer orderId;
    String orderReference;
    Customer customer;

}
