package com.younes.eccomerc.product;

import java.util.List;

import org.springframework.stereotype.Service;

import com.younes.eccomerc.category.Category;
import com.younes.eccomerc.category.CategoryRepo;

import lombok.RequiredArgsConstructor;

/**
 * ProductMapper
 */
@Service
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryRepo categoryRepo;

    public Product toProduct(ProductRequest productRequest) {
        // The category must exist, otherwise the foreign key insert fails and the
        // saved product ends up with a null category, which breaks every read path.
        // The previous code built a stub Category from the *product* id, so the
        // submitted categoryId was never read at all.
        var category = categoryRepo.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ProductCategoryNotFoundException(productRequest.getCategoryId()));

        return Product.builder()
                    .name(productRequest.getName())
                    .description(productRequest.getDescription())
                    .availableQuantity(productRequest.getAvailableQuantity())
                    .price(productRequest.getPrice())
                    .category(category)
                    .build();
    }

    public ProductResponse toProductResponse(Product product) {
        var category = product.getCategory();
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getAvailableQuantity(),
            product.getPrice(),
            category == null ? null : category.getId(),
            category == null ? null : category.getName(),
            category == null ? null : category.getDescription()
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
