package com.younes.ecommerce.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.younes.ecommerce.Customer;

/**
 * CustomerRepo
 */
public interface CustomerRepo extends MongoRepository<Customer, String>{

    Optional<Customer> findByKeycloakId(String keycloakId);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);
}