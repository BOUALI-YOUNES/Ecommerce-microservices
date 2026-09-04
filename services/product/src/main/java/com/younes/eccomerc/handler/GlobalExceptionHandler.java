package com.younes.eccomerc.handler;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.younes.eccomerc.Exception.ProductPurchasException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ProductPurchasException.class)
    public ResponseEntity<String> handle(ProductPurchasException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handle(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        var errors = new HashMap<String,String>();
        e.getBindingResult().getAllErrors()
                                            .forEach(error -> {
                                                String fieldName = ((FieldError)error).getField();
                                                String errorMessage = error.getDefaultMessage();
                                                errors.put(fieldName, errorMessage);
                                            }
                                                        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(errors));
    }
}
