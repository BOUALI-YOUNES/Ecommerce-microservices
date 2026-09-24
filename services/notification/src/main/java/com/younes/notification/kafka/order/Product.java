package com.younes.notification.kafka.order;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Product
 */
@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter  
public class Product {
    private Integer id;
    private String name;
    private String descreption;
    private BigDecimal price;
    private double quantity;
}
