package com.younes.eccomerc.product;

import java.util.List;

import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import com.younes.eccomerc.category.Category;

import jakarta.validation.constraints.NotNull;

/**
 * ProductMapper
 */
@Service
public class ProductMapper {

    public Product toProduct(ProductRequest productRequest) {
       return Product.builder()
                    .id(productRequest.getId())
                    .name(productRequest.getName())
                    .description(productRequest.getDescription())
                    .availableQuantity(productRequest.getAvailableQuantity())
                    .price(productRequest.getPrice())
                    .category(Category.builder().id(productRequest.getId()).build())
                    .build();
    }

    public ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getAvailableQuantity(),
            product.getPrice(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getCategory().getDescription()
        );
    }

    public ProductPurchasResponse toProductPurchaseReponse(Product product,
            double quantity) {
        return new ProductPurchasResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            quantity
        );
    }
    

}
