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

    private String orderReference;
    private BigDecimal amount;
    private PaymentMethode paymentMethode;
    private String customerFirstName;
    private String customerlastName;
    private String customerEmail;
}
