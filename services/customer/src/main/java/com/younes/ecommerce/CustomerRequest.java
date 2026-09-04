package com.younes.ecommerce;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CustomerResquest
 */
@AllArgsConstructor
@NoArgsConstructor 
@Getter
@Setter
public class CustomerRequest {
    private String id;
    @NotBlank(message = "customer first name is required !")
    private String first_name;
    @NotBlank(message = "customer last name is required !")
    private String last_name;
    @Email(message = "customer email is not valid  !")
    private String email;
    private Address address;

}
