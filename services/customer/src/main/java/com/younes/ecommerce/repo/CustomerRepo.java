package com.younes.ecommerce.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.younes.ecommerce.Customer;

/**
 * CustomerRepo
 */
public interface CustomerRepo extends MongoRepository<Customer, String>{

}
