package com.younes.notification.kafka.payment;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PaymentConfirmation
 */
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter  
public class PaymentConfirmation {

    private String productReference;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String customerFirstname;
    private String customerLastname;
    private String customerEmail;

}
