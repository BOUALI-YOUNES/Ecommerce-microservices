package com.younes.notification.kafka;

import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.younes.notification.email.EmailService;
import com.younes.notification.kafka.order.OrderConfirmation;
import com.younes.notification.kafka.payment.PaymentConfirmation;
import com.younes.notification.notification.Notification;
import com.younes.notification.notification.NotificationRepo;
import com.younes.notification.notification.NotificationType;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j 
public class NotificationConsumer {
    private final NotificationRepo notificationRepo;
    private final EmailService emailService;

    @KafkaListener(topics = "payment-topic")
    public void consumePaymentSuccessNotification(PaymentConfirmation paymentConfirmation) throws MessagingException {
        log.info(String.format("consuming message from the payment-topic topic :: %s", paymentConfirmation));
        notificationRepo.save(
            Notification.builder()
                .notificationType(NotificationType.PAYMENT_CONFIRMATION)
                .notificDateTime(LocalDateTime.now())
                .paymentConfirmation(paymentConfirmation)
                .build()
            );

        String customerName = paymentConfirmation.getCustomerFirstname() + " " + paymentConfirmation.getCustomerLastname();
        emailService.sendPaymentSuccessEmail(paymentConfirmation.getCustomerEmail(), customerName, paymentConfirmation.getAmount(), paymentConfirmation.getProductReference());
    }

    @KafkaListener(topics = "order-topic")
    public void consumeOrderNotification(OrderConfirmation orderConfirmation) throws MessagingException {
        log.info(String.format("consuming message from the order-topic topic :: %s", orderConfirmation));
        notificationRepo.save(
            Notification.builder()
                .notificationType(NotificationType.ORDER_CONFIRMATION)
                .notificDateTime(LocalDateTime.now())
                .orderConfirmation (orderConfirmation)
                .build()
            );
        String customerName = orderConfirmation.getCustomer().getFirstname() + " " + orderConfirmation.getCustomer().getLastname();
        emailService.sendOrderConfirmationEmail(orderConfirmation.getCustomer().getEmail(), customerName, orderConfirmation.getTotaleAmount(), orderConfirmation.getOrderReference() , orderConfirmation.getProducts());
    }

    
}
