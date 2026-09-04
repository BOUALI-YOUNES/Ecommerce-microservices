package com.younes.eccomerc.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ProductRepo
 */
public interface ProductRepo extends JpaRepository<Product , Integer>{

    List<Product> findAllByIdInOrderById(List<Integer> productIds);

}
