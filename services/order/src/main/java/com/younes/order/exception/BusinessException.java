package com.younes.order.exception;

/**
 * BusinessException
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String msg) {
        super(msg);
    }
}