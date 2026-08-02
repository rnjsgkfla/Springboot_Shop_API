package com.skala.shop.controller;

import com.skala.shop.dto.WishResponse;
import com.skala.shop.service.WishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
@Tag(name = "찜", description = "로그인한 고객의 찜 목록 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class WishController {

    private final WishService wishService;

    @GetMapping
    @Operation(summary = "찜 목록 조회", description = "로그인한 고객이 찜한 상품 목록을 조회합니다.")
    public ResponseEntity<List<WishResponse>> findAll(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
            wishService.findAll(authentication.getName())
        );
    }

    @PostMapping("/{productId}")
    @Operation(summary = "상품 찜하기", description = "상품 ID에 해당하는 상품을 내 찜 목록에 추가합니다.")
    public ResponseEntity<WishResponse> create(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(wishService.create(authentication.getName(), productId));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "찜 취소", description = "상품 ID에 해당하는 상품을 내 찜 목록에서 삭제합니다.")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        wishService.delete(authentication.getName(), productId);
        return ResponseEntity.noContent().build();
    }
}
