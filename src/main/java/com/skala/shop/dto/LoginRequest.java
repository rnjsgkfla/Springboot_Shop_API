package com.skala.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "고객 ID는 필수입니다.")
    String customerId,
    @NotBlank(message = "비밀번호는 필수입니다.")
    String customerPassword
) {
}
