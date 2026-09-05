package com.skala.shop.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.genai.errors.ClientException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void geminiQuotaErrorIsReturnedAsTooManyRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ai/chat");
        Exception exception = new RuntimeException(
                "Failed to generate content",
                new ClientException(429, "RESOURCE_EXHAUSTED", "Quota exceeded")
        );

        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(
                exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("AI_QUOTA_EXCEEDED");
        assertThat(response.getBody().message()).contains("무료 사용량");
    }
}
