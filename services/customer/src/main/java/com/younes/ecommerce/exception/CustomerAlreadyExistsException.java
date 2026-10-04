package com.younes.ecommerce.exception;

/**
 * Raised when a create would collide with an existing customer, either because the
 * authenticated user already has a profile or because the email is taken.
 */
public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}