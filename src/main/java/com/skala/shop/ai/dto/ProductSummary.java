package com.skala.shop.ai.dto;

import com.skala.shop.entity.Product;

public record ProductSummary(
        Long id,
        String name,
        Double price,
        Integer stockQuantity,
        String description,
        String category,
        String brand,
        String tags
) {
    public static ProductSummary from(Product product) {
        return new ProductSummary(
                product.getId(),
                product.getProductName(),
                product.getProductPrice(),
                product.getStockQuantity(),
                product.getDescription(),
                product.getCategory(),
                product.getBrand(),
                product.getTags()
        );
    }
}
