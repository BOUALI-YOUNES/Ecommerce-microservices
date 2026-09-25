package com.younes.order.kafka;

import java.math.BigDecimal;
import java.util.List;

import com.younes.order.models.PaymentMethod;
import com.younes.order.models.PurchaseResponse;
import com.younes.order.models.customer.CustomerResponse;

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

    String orderReference;
    BigDecimal totaleAmount;
    PaymentMethod paymentMethod;
    CustomerResponse customer;
    List<PurchaseResponse> products;

}
