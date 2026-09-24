package com.younes.ecommerce;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document
@Builder
public class Customer {

    @Id
    private String id;
    private String first_name;
    private String last_name;
    @Email (message = "The email is not correctlly formated !")
    @NotNull (message = "The email is required!")
    private String email;
    private Address address;
}
