package com.younes.eccomerc.product;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Integer> createProduct(@RequestBody @Valid ProductRequest productRequest) {
        return productService.createProduct(productRequest);
    }

    @PostMapping("/purchas")
    public ResponseEntity<List<ProductPurchasResponse>> purchasProducts(@RequestBody List<ProductPurchasRequest> productPurchasRequest) {
        return ResponseEntity.ok(productService.purchasProducts(productPurchasRequest));
    }

    @GetMapping("/{product-id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("product-id") Integer productId) {
        return ResponseEntity.ok(productService.getById(productId));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    
}
