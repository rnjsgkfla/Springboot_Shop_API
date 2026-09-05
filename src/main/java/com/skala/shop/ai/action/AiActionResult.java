package com.skala.shop.ai.action;

public record AiActionResult(
        AiActionType type,
        String message,
        Object result
) {
}
