package com.younes.eccomerc.product;

/**
 * ProductCategoryNotFoundException
 */
public class ProductCategoryNotFoundException extends RuntimeException {

    public ProductCategoryNotFoundException(Integer categoryId) {
        super("No category found with the ID :: " + categoryId);
    }
}