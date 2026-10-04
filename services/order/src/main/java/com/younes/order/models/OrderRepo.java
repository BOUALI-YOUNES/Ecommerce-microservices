package com.younes.order.models;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OrderRepo
 */
public interface OrderRepo extends JpaRepository<Order , Integer> {

    boolean existsByReference(String reference);

    List<Order> findAllByCustomerId(String customerId);

    Order findByIdAndCustomerId(Integer id, String customerId);
}