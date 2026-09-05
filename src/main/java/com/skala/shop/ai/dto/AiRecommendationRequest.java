package com.skala.shop.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiRecommendationRequest(@NotBlank String query) {
}
