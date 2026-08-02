package com.skala.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "서버 상태", description = "서버 실행 상태 확인 API")
public class HealthController {

    @GetMapping
    @Operation(summary = "서버 상태 확인", description = "서버가 정상적으로 실행 중인지 확인합니다.")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "skala-shop-api");
    }
}
