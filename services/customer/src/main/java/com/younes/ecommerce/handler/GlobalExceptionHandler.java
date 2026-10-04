package com.younes.ecommerce.handler;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.younes.ecommerce.exception.CustomerAccessDeniedException;
import com.younes.ecommerce.exception.CustomerAlreadyExistsException;
import com.younes.ecommerce.exception.CustomerException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<String> handle(CustomerException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<String> handle(CustomerAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
    }

    @ExceptionHandler(CustomerAccessDeniedException.class)
    public ResponseEntity<String> handle(CustomerAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handle(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("The request body could not be read");
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