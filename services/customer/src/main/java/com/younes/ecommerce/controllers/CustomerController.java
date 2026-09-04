package com.younes.ecommerce.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.younes.ecommerce.CustomerRequest;
import com.younes.ecommerce.CustomerResponse;
import com.younes.ecommerce.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<String> createCustomer(@RequestBody @Valid CustomerRequest request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }

    @PutMapping
    public ResponseEntity<Void> editCustomer(@RequestBody @Valid CustomerRequest request) {
        customerService.editCustomer(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> customers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/exists/{customerId}")
    public ResponseEntity<Boolean> customerExistsById(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(customerService.existsById(customerId));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> findCustomerById(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(customerService.findById(customerId));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("customerId") String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.accepted().build();
    }

}
