package com.younes.payment.payement;

import org.springframework.stereotype.Service;

import com.younes.payment.notification.NotificationProducer;
import com.younes.payment.notification.PaymentNotificationRequest;

import lombok.RequiredArgsConstructor;

/**
 * PaymentService
 */
@RequiredArgsConstructor
@Service
public class PaymentService {
    private final PaymentRepo paymentRepo;
    private final PaymentMapper paymentMapper;
    private final NotificationProducer notificationProducer;

    public Integer createPayement(PaymentRequest reqeust) {
        var payemet = paymentRepo.save(paymentMapper.toPayment(reqeust));
        notificationProducer.sendNotification(
            new PaymentNotificationRequest(
                reqeust.getOrderReference(), 
                reqeust.getAmount(), 
                reqeust.getPaymentMethode(), 
                reqeust.customer.getFirstname(), 
                reqeust.customer.getLastname(), 
                reqeust.customer.getEmail())
        );
        return payemet.getId();
    }

}
