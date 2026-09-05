package com.skala.shop.ai.action;

import java.time.Instant;

public record PendingAiAction(
        String confirmationToken,
        AiActionType type,
        Long productId,
        int quantity,
        Instant expiresAt,
        String confirmationMessage
) {
}
