package com.skala.shop.service;

import com.skala.shop.dto.WishResponse;
import com.skala.shop.entity.Customer;
import com.skala.shop.entity.Product;
import com.skala.shop.entity.Wish;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.CustomerRepository;
import com.skala.shop.repository.ProductRepository;
import com.skala.shop.repository.WishRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishService {

    private final WishRepository wishRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public List<WishResponse> findAll(String customerId) {
        Customer customer = getCustomer(customerId);
        return wishRepository.findByCustomer(customer).stream()
            .map(WishResponse::from)
            .toList();
    }

    @Transactional
    public WishResponse create(String customerId, Long productId) {
        Customer customer = getCustomer(customerId);
        Product product = getProduct(productId);

        if (wishRepository.findByCustomerAndProduct(customer, product).isPresent()) {
            throw new BusinessException(ErrorCode.WISH_DUPLICATED);
        }

        return WishResponse.from(
            wishRepository.save(new Wish(customer, product))
        );
    }

    @Transactional
    public void delete(String customerId, Long productId) {
        Customer customer = getCustomer(customerId);
        Product product = getProduct(productId);
        Wish wish = wishRepository.findByCustomerAndProduct(customer, product)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.WISH_NOT_FOUND)
            );
        wishRepository.delete(wish);
    }

    private Customer getCustomer(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND)
            );
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );
    }
}
