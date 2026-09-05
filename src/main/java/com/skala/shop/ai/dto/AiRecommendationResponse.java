package com.skala.shop.ai.dto;

import java.util.List;

public record AiRecommendationResponse(List<RecommendedProduct> products) {

    public record RecommendedProduct(
            Long productId,
            String productName,
            Double productPrice,
            Integer stockQuantity,
            String reason
    ) {
    }
}
