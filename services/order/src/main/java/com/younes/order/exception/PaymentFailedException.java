package com.younes.order.exception;

/**
 * Raised when the payment step fails, so the caller can distinguish a payment outage
 * from a validation problem. The surrounding transaction rolls back, and the stock
 * reserved with the product service is released.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}