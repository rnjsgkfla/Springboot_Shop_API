package com.skala.shop.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInMinutes
) {
}
