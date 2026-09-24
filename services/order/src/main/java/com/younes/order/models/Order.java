package com.younes.order.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor 
@NoArgsConstructor 
@Getter 
@Setter 
@Builder 
@EntityListeners(AuditingEntityListener.class)
@Entity 
@Table (name = "customer_order")
public class Order {
    @Id 
    @GeneratedValue 
    private Integer id;
    private String reference;
    private BigDecimal totalAmount ; 
    @Enumerated
    private PaymentMethod paymentMethod;
    private String customerId;
    @OneToMany (mappedBy = "order")
    private List<OrderLine> orderLines;
    @CreatedDate 
    @Column(updatable = false, nullable = false )
    private LocalDateTime createdAt;
    @LastModifiedDate 
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;
}
