package com.skala.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(
    @NotBlank(message = "고객 ID는 필수입니다.")
    String customerId,
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, message = "비밀번호는 4자 이상이어야 합니다.")
    String customerPassword,
    String referrerId
) {
}
