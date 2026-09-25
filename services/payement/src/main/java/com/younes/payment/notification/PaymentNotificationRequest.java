package com.younes.payment.notification;

import java.math.BigDecimal;

import com.younes.payment.payement.PaymentMethode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PaymentNotificationRequest
 */
@AllArgsConstructor 
@Getter 
public class PaymentNotificationRequest {

    private String productReference;
    private BigDecimal amount;
    private PaymentMethode paymentMethod;
    private String customerFirstname;
    private String customerLastname;
    private String customerEmail;
}
