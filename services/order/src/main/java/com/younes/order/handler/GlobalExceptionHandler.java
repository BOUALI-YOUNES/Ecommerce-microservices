package com.younes.order.handler;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import com.younes.order.exception.BusinessException;
import com.younes.order.exception.PaymentFailedException;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handle(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handle(FeignException e) {
        // HttpStatus.valueOf throws for codes outside the known enum, which would mask
        // the real failure as a 500. resolve() returns null instead.
        HttpStatus status = HttpStatus.resolve(e.status()) != null
                ? HttpStatus.resolve(e.status())
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status)
                    .body(e.getMessage());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<String> handle(PaymentFailedException e) {
        // The downstream payment service failed, so this is a gateway/upstream problem
        // rather than a malformed request.
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(e.getMessage());
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handle(RestClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                    .body(e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handle(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        var errors = new HashMap<String, String>();
        e.getBindingResult().getAllErrors()
            .forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handle(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("The request body could not be read");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
}