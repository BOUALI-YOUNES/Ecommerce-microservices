package com.younes.notification.kafka.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer
 */
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter  
public class Customer {
    private String id;
    private String firstname;
    private String lastname;
    private String email;
}
