package com.younes.order.models;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.younes.order.exception.BussenisException;
import com.younes.order.kafka.OrderConfirmation;
import com.younes.order.kafka.OrderProducer;
import com.younes.order.models.customer.CustomerClient;
import com.younes.order.payment.PaymentClient;
import com.younes.order.payment.PaymentRequest;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class OrderService {

    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderRepo orderRepo;
    private final OrderMapper mapper;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;

    public Integer createOrder(OrderRequest orderRequest) {
       //check if the customer exists using OpenFeign
       var customer = this.customerClient.findCustomerById(orderRequest.getCustomerId())
                                    .orElseThrow(() -> new BussenisException("Order cannot be created :: No customer with ID :: " + orderRequest.getCustomerId()));

        //purchase the products
        var purchasedProducts = this.productClient.purchaseProduct(orderRequest.getProducts());
         
        //persist the order
        var order = this.orderRepo.save(mapper.toOrder(orderRequest));

        //persist the orderLine
        for(PurchaseRequest purchaseRequest : orderRequest.getProducts()) {
            orderLineService.saveOrderLine(new OrderLineRequest(
                null,order.getId(),purchaseRequest.getProductId(),purchaseRequest.getQuantity()
            ));
        }

        // payment 
        var paymentRequest = new PaymentRequest(
           orderRequest.getId(), orderRequest.getAmount(),orderRequest.getPaymentMethod(), order.getId(),order.getReference(),customer
        );
        paymentClient.requestOrderPayment(paymentRequest);

        // send the order confirmation to the notification service
        orderProducer.sendOrderConfirmation(
            new OrderConfirmation(
                orderRequest.getReference(),
                orderRequest.getAmount(),
                orderRequest.getPaymentMethod(),
                customer,
                purchasedProducts
            )
        );

        return order.getId(); 

    }
    public List<OrderResponse> findAll() {
        return orderRepo.findAll()
                    .stream()
                    .map(mapper::fromOrder)
                    .collect(Collectors.toList());
    }
    public OrderResponse findById(Integer orderId) {
        return orderRepo.findById(orderId).map(mapper::fromOrder).orElseThrow(() -> new EntityNotFoundException("No order found with the provided ID :: " + orderId) );
    }
    
}
