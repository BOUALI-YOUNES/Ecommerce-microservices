package com.younes.ecommerce.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CustomerException
 */
@Data
@EqualsAndHashCode
public class CustomerException extends RuntimeException{
    private final String message;
}
