package com.younes.order.payment;

import java.math.BigDecimal;

import com.younes.order.models.PaymentMethod;
import com.younes.order.models.customer.CustomerResponse;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PaymentRequest
 */
@Getter 
@AllArgsConstructor 
public class PaymentRequest {
    private Integer id;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    Integer orderId;
    String orderReference;
    CustomerResponse customer;

}
