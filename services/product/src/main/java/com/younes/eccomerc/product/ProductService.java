package com.younes.eccomerc.product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.younes.eccomerc.Exception.ProductPurchasException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * ProductService
 */

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    public ResponseEntity<Integer> createProduct(ProductRequest productRequest) {
        var product = productMapper.toProduct(productRequest);
        return ResponseEntity.ok(productRepo.save(product).getId());
    }

    /**
     * Reserves stock for every requested line.
     *
     * Each decrement is a single conditional UPDATE, so concurrent purchases cannot
     * oversell. The whole method runs in one transaction: if any line has insufficient
     * stock the exception rolls back every decrement already applied, instead of
     * leaving the first lines of a rejected order consumed.
     *
     * Duplicate product ids in one request are summed rather than rejected.
     */
    @Transactional
    public List<ProductPurchasResponse> purchasProducts(List<ProductPurchasRequest> productPurchasRequest) {
        Map<Integer, Double> requestedQuantities = new LinkedHashMap<>();
        for (ProductPurchasRequest request : productPurchasRequest) {
            if (request.getQuantity() <= 0) {
                throw new ProductPurchasException(
                        "The quantity must be greater than zero for product with the ID :: "
                                + request.getProductId());
            }
            requestedQuantities.merge(request.getProductId(), request.getQuantity(), Double::sum);
        }

        var productIds = List.copyOf(requestedQuantities.keySet());
        var storedProducts = productRepo.findAllByIdInOrderById(productIds);
        if (storedProducts.size() != requestedQuantities.size()) {
            throw new ProductPurchasException("One or more products does not exists !");
        }

        for (var entry : requestedQuantities.entrySet()) {
            int updatedRows = productRepo.decrementStock(entry.getKey(), entry.getValue());
            if (updatedRows == 0) {
                throw new ProductPurchasException(
                        "Cannot performe this purchase , Quantiy is not available for product with the ID :: "
                                + entry.getKey());
            }
        }

        // Re-read so the response carries authoritative post-decrement state.
        var refreshedProducts = productRepo.findAllByIdInOrderById(productIds);
        var purchasProducts = new ArrayList<ProductPurchasResponse>();
        for (var product : refreshedProducts) {
            double quantity = requestedQuantities.get(product.getId());
            purchasProducts.add(productMapper.toProductPurchaseReponse(product, quantity));
        }
        return purchasProducts;
    }

    /**
     * Returns reserved stock to the catalogue. Used to compensate an order that was
     * rolled back after the reservation had already succeeded.
     */
    @Transactional
    public void releaseProducts(List<ProductPurchasRequest> productPurchasRequest) {
        for (ProductPurchasRequest request : productPurchasRequest) {
            productRepo.incrementStock(request.getProductId(), request.getQuantity());
        }
    }

    public ProductResponse getById(Integer productId) {
        return productRepo.findById(productId)
                    .map(productMapper::toProductResponse)
                    .orElseThrow(() -> new EntityNotFoundException("No product found with the ID :: " + productId));
    }

    public List<ProductResponse> findAll() {
        return productRepo.findAll()
                    .stream()
                    .map(productMapper::toProductResponse)
                    .collect(Collectors.toList());
    }

}