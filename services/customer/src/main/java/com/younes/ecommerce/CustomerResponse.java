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
    private String first_name;
    private String last_name;
    private String email;
    private Address address;

}
