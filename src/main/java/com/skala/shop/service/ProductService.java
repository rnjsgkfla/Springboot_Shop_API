package com.skala.shop.service;

import com.skala.shop.entity.Product;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 전체 상품 조회
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // ID로 상품 상세 조회
    public Product findById(Long id) {
        return getProduct(id);
    }

    // 상품 등록
    @Transactional
    public Product create(Product product) {
        validateProduct(product);

        // 같은 상품명이 이미 있는지 확인
        if (productRepository
                .findByProductName(product.getProductName())
                .isPresent()) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_DUPLICATED
            );
        }

        // IDENTITY 방식의 신규 엔티티는 ID를 null로 설정
        product.setId(null);

        return productRepository.save(product);
    }

    // 상품 수정
    @Transactional
    public Product update(Long id, Product product) {
        validateProduct(product);

        Product savedProduct = getProduct(id);

        // 다른 상품이 같은 이름을 사용하는지 확인
        productRepository
                .findByProductName(product.getProductName())
                .filter(foundProduct ->
                        !foundProduct.getId().equals(id))
                .ifPresent(foundProduct -> {
                    throw new BusinessException(
                            ErrorCode.PRODUCT_DUPLICATED
                    );
                });

        savedProduct.setProductName(
                product.getProductName()
        );
        savedProduct.setProductPrice(
                product.getProductPrice()
        );

        return productRepository.save(savedProduct);
    }

    // 상품 삭제
    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    // 상품 조회 공통 메서드
    private Product getProduct(Long id) {
        if (id == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );
    }

    // 상품명과 가격 검증
    private void validateProduct(Product product) {
        if (product == null
                || product.getProductName() == null
                || product.getProductName().isBlank()
                || product.getProductPrice() == null
                || product.getProductPrice() <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }
}