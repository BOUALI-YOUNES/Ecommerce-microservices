package com.younes.ecommerce;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CustomerResquest
 *
 * <p>There is deliberately no id field. It was previously copied straight onto the
 * Mongo document, and because Mongo save() with a non-null _id replaces the document,
 * the unauthenticated POST /api/v1/customers endpoint could overwrite any existing
 * customer. The identifier is now generated, and updates address a customer through
 * {@code /api/v1/customers/me} instead of a client-supplied id.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerRequest {
    @NotBlank(message = "customer first name is required !")
    private String firstname;
    @NotBlank(message = "customer last name is required !")
    private String lastname;
    @Email(message = "customer email is not valid  !")
    private String email;
    private Address address;

}