package com.younes.notification.kafka.order;

import java.math.BigDecimal;
import java.util.List;

import com.younes.notification.kafka.payment.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OrderConfirmation
 */
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter 
public class OrderConfirmation {

    private String orderReference;
    private BigDecimal totaleAmount;
    private PaymentMethod paymentMethod;
    private Customer customer;
    List<Product> products; 
}
