package com.younes.order.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.younes.order.exception.BusinessException;
import com.younes.order.exception.PaymentFailedException;
import com.younes.order.kafka.OrderConfirmation;
import com.younes.order.kafka.OrderProducer;
import com.younes.order.models.customer.CustomerClient;
import com.younes.order.models.customer.CustomerResponse;
import com.younes.order.payment.PaymentClient;
import com.younes.order.payment.PaymentRequest;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final OrderRepo orderRepo;
    private final OrderLineService orderLineService;
    private final OrderMapper mapper;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;
    private final OrderCompensation orderCompensation;

    /**
     * Places an order for the authenticated caller.
     *
     * <p>The order total is computed here from the product prices returned by the
     * product service. The previous implementation copied the amount from the request
     * body straight through to the payment request and the confirmation email, so a
     * client could order a 999.99 laptop for 0.01.
     *
     * <p>The customer is resolved from the caller's forwarded token rather than from a
     * request field, so an authenticated user cannot place an order against somebody
     * else's customer id.
     *
     * <p>Order and order lines are persisted in one transaction. The product purchase
     * and the payment call are remote operations and cannot join that transaction, so
     * a failure after stock was reserved would otherwise leave stock consumed with no
     * order. {@link OrderCompensation} restores the reservation in that case.
     */
    @Transactional
    public Integer createOrder(OrderRequest orderRequest, String customerId) {
        CustomerResponse customer = resolveCustomer();

        if (orderRepo.existsByReference(orderRequest.getReference())) {
            throw new BusinessException(
                    "An order with the reference " + orderRequest.getReference() + " already exists");
        }

        // Reserve stock first. The product service returns the authoritative unit
        // prices, which is what the total is derived from.
        List<PurchaseResponse> purchasedProducts = productClient.purchaseProduct(orderRequest.getProducts());

        // If anything below fails, give the reserved stock back.
        orderCompensation.releaseOnRollback(orderRequest.getProducts());

        BigDecimal totalAmount = computeTotalAmount(purchasedProducts);

        var order = orderRepo.save(mapper.toOrder(orderRequest, customerId, totalAmount));

        for (PurchaseRequest purchaseRequest : orderRequest.getProducts()) {
            orderLineService.saveOrderLine(new OrderLineRequest(
                null, order.getId(), purchaseRequest.getProductId(), purchaseRequest.getQuantity()
            ));
        }

        var paymentRequest = new PaymentRequest(
                totalAmount, orderRequest.getPaymentMethod(), order.getId(), order.getReference(), customer
        );

        try {
            paymentClient.requestOrderPayment(paymentRequest);
        } catch (RuntimeException e) {
            // Let the transaction roll back so no unpaid order is left behind, then
            // release the reserved stock before surfacing the failure.
            throw new PaymentFailedException(
                    "The payment could not be processed, the order was not created :: " + e.getMessage(), e);
        }

        order.markPaid();

        orderProducer.sendOrderConfirmation(
            new OrderConfirmation(
                order.getReference(),
                totalAmount,
                orderRequest.getPaymentMethod(),
                customer,
                purchasedProducts
            )
        );

        return order.getId();
    }

    /**
     * Sums price x quantity over every purchased line.
     */
    private BigDecimal computeTotalAmount(List<PurchaseResponse> purchasedProducts) {
        BigDecimal total = purchasedProducts.stream()
                .map(purchase -> purchase.getPrice()
                        .multiply(BigDecimal.valueOf(purchase.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private CustomerResponse resolveCustomer() {
        try {
            return customerClient.findCurrentCustomer();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(
                    "No customer profile is associated with the authenticated user");
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepo.findAll()
                    .stream()
                    .map(mapper::fromOrder)
                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAllByCustomerId(String customerId) {
        return orderRepo.findAllByCustomerId(customerId)
                    .stream()
                    .map(mapper::fromOrder)
                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Integer orderId) {
        return orderRepo.findById(orderId).map(mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException("No order found with the provided ID :: " + orderId));
    }

    @Transactional(readOnly = true)
    public OrderResponse findByIdAndCustomerId(Integer orderId, String customerId) {
        return Optional.ofNullable(orderRepo.findByIdAndCustomerId(orderId, customerId))
                .map(mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException("No order found with the provided ID :: " + orderId));
    }
}