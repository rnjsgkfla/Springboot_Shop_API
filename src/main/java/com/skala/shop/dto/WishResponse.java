package com.skala.shop.dto;

import com.skala.shop.entity.Wish;

public record WishResponse(
    Long productId,
    String productName,
    Double productPrice,
    Integer stockQuantity
) {
    public static WishResponse from(Wish wish) {
        return new WishResponse(
            wish.getProduct().getId(),
            wish.getProduct().getProductName(),
            wish.getProduct().getProductPrice(),
            wish.getProduct().getStockQuantity()
        );
    }
}
