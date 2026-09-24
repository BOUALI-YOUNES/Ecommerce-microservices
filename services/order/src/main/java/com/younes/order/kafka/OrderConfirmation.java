package com.younes.order.kafka;

import java.math.BigDecimal;
import java.util.List;

import com.younes.order.models.PaymentMethod;
import com.younes.order.models.PurchaseResponse;
import com.younes.order.models.customer.CustomerResponse;

import lombok.AllArgsConstructor;

/**
 * OrderConfirmation
 */
@AllArgsConstructor 
public class OrderConfirmation {

    String orderReference;
    BigDecimal totalAmount;
    PaymentMethod payementMethode;
    CustomerResponse customerResponse;
    List<PurchaseResponse> products;

}
