package com.younes.notification.notification;

import java.time.LocalDateTime;

import com.younes.notification.kafka.order.OrderConfirmation;
import com.younes.notification.kafka.payment.PaymentConfirmation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor 
@NoArgsConstructor 
@Getter
@Setter 
@Document
@Builder 
public class  Notification {
    
    @Id
    private Integer id;
    private NotificationType notificationType;
    private LocalDateTime notificDateTime;
    private OrderConfirmation orderConfirmation;
    private PaymentConfirmation paymentConfirmation;
}
