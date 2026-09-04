package com.younes.eccomerc.product;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.younes.eccomerc.Exception.ProductPurchasException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
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

    public List<ProductPurchasResponse> purchasProducts(List<ProductPurchasRequest> productPurchasRequest) {
        var productIds = productPurchasRequest
                                    .stream()
                                    .map(request -> request.getProductId())
                                    .toList();
        var storedProducts = productRepo.findAllByIdInOrderById(productIds);
        if(productIds.size() != storedProducts.size()) {
            throw new ProductPurchasException("One or more products does not exists !");
        }

        var sortedRequest = productPurchasRequest.stream().sorted(Comparator.comparing(product -> product.getProductId())).toList();
        var purchasProducts = new ArrayList<ProductPurchasResponse>();
        for(int i = 0 ; i < sortedRequest.size() ; i++) {

            var product = storedProducts.get(i);  // stored products comming form the db via the repo
            var productRequest = sortedRequest.get(i); // client request products

            if(product.getAvailableQuantity() < productRequest.getQuantity()) {
                throw new ProductPurchasException("Cannot performe this purchase , Quantiy is not available for product with the ID :: " + productRequest.getProductId());
            } 
            // update the available quantity for the purchased product
            var newAvailableQuantity = product.getAvailableQuantity() - productRequest.getQuantity();
            product.setAvailableQuantity(newAvailableQuantity);
            productRepo.save(product); //saving the new update
            purchasProducts.add(productMapper.toProductPurchaseReponse(product , productRequest.quantity));
        }
        return purchasProducts;
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
