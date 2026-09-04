package com.younes.ecommerce;

import org.springframework.validation.annotation.Validated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Validated
@Builder
public class Address {
    private String street;
    private String houseNumber;
    private String zipCode;
    
}
