package com.younes.payment.payement;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;


@Validated 
@Getter  
public class Customer {
    @NotNull 
    private String id;
    @NotNull
    private String firstname;
    @NotNull
    private String lastname;
    @NotNull
    @Email 
    private String email;

}
