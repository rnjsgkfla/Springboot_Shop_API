package com.skala.shop.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String message,
        String conversationId
) {
}
