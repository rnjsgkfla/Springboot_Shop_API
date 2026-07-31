package com.skala.shop.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String code,
    String message,
    String path
) {
}