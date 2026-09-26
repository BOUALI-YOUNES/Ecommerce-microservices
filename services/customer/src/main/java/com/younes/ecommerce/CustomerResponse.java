package com.younes.ecommerce;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CustomerResponse
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {
     private String id;
    private String firstname;
    private String lastname;
    private String email;
    private Address address;

}
