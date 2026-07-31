package com.skala.shop.controller;

import com.skala.shop.entity.Product;
import com.skala.shop.service.ProductService;
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
public class ProductController {

    private final ProductService productService;

    // 전체 상품 목록 조회
    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(
                productService.findAll()
        );
    }

    // 상품 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                productService.findById(id)
        );
    }

    // 상품 등록
    @PostMapping
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
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        productService.delete(id);

        return ResponseEntity.noContent().build();
    }
}