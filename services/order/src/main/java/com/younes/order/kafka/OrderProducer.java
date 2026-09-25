package com.younes.order.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@RequiredArgsConstructor 
@Slf4j 
public class OrderProducer {

    private final KafkaTemplate<?, ?> kafkaTemplate;

    public void sendOrderConfirmation(OrderConfirmation orderConfirmation) {
        log.info("sending order confirmation");
        Message<OrderConfirmation> message = MessageBuilder
                            .withPayload(orderConfirmation)
                            .setHeader(KafkaHeaders.TOPIC, "order-topic")
                            .build();
        kafkaTemplate.send(message);
    
    }
    
}
