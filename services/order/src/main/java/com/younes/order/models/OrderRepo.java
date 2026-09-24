package com.younes.order.models;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OrderRepo
 */
public interface OrderRepo extends JpaRepository<Order , Integer> {

}
