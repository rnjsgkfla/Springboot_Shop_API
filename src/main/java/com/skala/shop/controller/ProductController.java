package com.skala.shop.controller;

import com.skala.shop.entity.Product;
import com.skala.shop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품", description = "상품 등록, 조회, 수정, 삭제 API")
public class ProductController {

    private final ProductService productService;

    // 전체 상품 목록 조회
    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "등록된 모든 상품과 재고를 조회합니다.")
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(
                productService.findAll()
        );
    }

    // 상품 상세 조회
    @GetMapping("/{id}")
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보와 재고를 조회합니다.")
    public ResponseEntity<Product> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                productService.findById(id)
        );
    }

    // 상품 등록
    @PostMapping
    @Operation(summary = "상품 등록", description = "상품명, 가격, 초기 재고를 입력해 새 상품을 등록합니다.")
    public ResponseEntity<Product> create(
            @RequestBody Product product
    ) {
        Product savedProduct =
                productService.create(product);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedProduct);
    }

    // 상품 수정
    @PutMapping("/{id}")
    @Operation(summary = "상품 수정", description = "상품 ID에 해당하는 상품명, 가격, 재고를 수정합니다.")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @RequestBody Product product
    ) {
        Product updatedProduct =
                productService.update(id, product);

        return ResponseEntity.ok(updatedProduct);
    }

    // 상품 삭제
    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제", description = "주문이나 찜에 사용되지 않은 상품을 삭제합니다.")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        productService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
