package com.younes.order.exception;

/**
 * The product service refused the purchase (insufficient stock, invalid payload).
 *
 * <p>Kept distinct from a plain BusinessException because the circuit breaker must not
 * treat a routine business rejection as an infrastructure failure. Counting them
 * opened the circuit after a handful of failed orders and then rejected every
 * subsequent order with "service unavailable", turning normal business use into an
 * outage.
 */
public class ProductPurchaseRejectedException extends BusinessException {
    public ProductPurchaseRejectedException(String message) {
        super(message);
    }
}
