package com.younes.ecommerce.exception;

/**
 * Raised when an authenticated caller tries to reach a customer profile that is not
 * their own. Mapped to 403 by the global handler.
 */
public class CustomerAccessDeniedException extends RuntimeException {
    public CustomerAccessDeniedException(String message) {
        super(message);
    }
}
