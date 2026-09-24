package com.younes.payment.payement;

import org.springframework.stereotype.Service;


/**
 * PaymentMapper
 */
@Service 
public class PaymentMapper {

    public Payment toPayment(PaymentRequest reqeust) {
       return Payment.builder()
            .id(reqeust.getId())
            .amount(reqeust.getAmount())
            .orderId(reqeust.getOrderId())
            .paymentMethode(reqeust.getPaymentMethode())
            .build();
    }

}
