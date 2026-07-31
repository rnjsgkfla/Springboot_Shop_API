package com.skala.shop.service;

import com.skala.shop.dto.OrderItemDto;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.entity.Customer;
import com.skala.shop.entity.OrderItem;
import com.skala.shop.entity.Product;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.CustomerProductRepository;
import com.skala.shop.repository.CustomerRepository;
import com.skala.shop.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private static final Double INITIAL_POINT = 1_000_000.0;

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerProductRepository customerProductRepository;

    // 전체 고객 목록 조회
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    // 고객 상세 정보와 주문상품 목록 조회
    public OrderListDto findById(String customerId) {
        Customer customer = getCustomer(customerId);

        List<OrderItemDto> products =
                customerProductRepository
                        .findByCustomerCustomerId(customerId)
                        .stream()
                        .map(this::convertToOrderItemDto)
                        .toList();

        return OrderListDto.builder()
                .customerId(customer.getCustomerId())
                .customerPoint(customer.getCustomerPoint())
                .products(products)
                .build();
    }

    // 고객 생성
    @Transactional
    public Customer create(Customer customer) {
        validateCustomerForCreate(customer);

        if (customerRepository.existsById(
                customer.getCustomerId())) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_CUSTOMER_ID
            );
        }

        customer.setCustomerPoint(INITIAL_POINT);

        return customerRepository.save(customer);
    }

    // 고객 로그인
    public Customer login(
            String customerId,
            String customerPassword
    ) {
        if (customerId == null
                || customerId.isBlank()
                || customerPassword == null
                || customerPassword.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.INVALID_CREDENTIALS
                        )
                );

        if (!customer.getCustomerPassword()
                .equals(customerPassword)) {
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS
            );
        }

        return customer;
    }

    // 고객 포인트 수정
    @Transactional
    public Customer update(Customer customer) {
        if (customer == null
                || customer.getCustomerId() == null
                || customer.getCustomerId().isBlank()
                || customer.getCustomerPoint() == null
                || customer.getCustomerPoint() < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        Customer savedCustomer =
                getCustomer(customer.getCustomerId());

        savedCustomer.setCustomerPoint(
                customer.getCustomerPoint()
        );

        return customerRepository.save(savedCustomer);
    }

    // 고객 삭제
    @Transactional
    public void delete(String customerId) {
        Customer customer = getCustomer(customerId);

        // 고객의 주문상품을 먼저 삭제해 외래키 오류 방지
        List<OrderItem> orderItems =
                customerProductRepository
                        .findByCustomerCustomerId(customerId);

        customerProductRepository.deleteAll(orderItems);
        customerRepository.delete(customer);
    }

    // 상품 주문
    @Transactional
    public OrderListDto placeOrder(
            String customerId,
            Long productId,
            int quantity
    ) {
        validateOrderRequest(productId, quantity);

        Customer customer = getCustomer(customerId);
        Product product = getProduct(productId);

        double orderPrice =
                product.getProductPrice() * quantity;

        if (customer.getCustomerPoint() < orderPrice) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_FUNDS
            );
        }

        // 고객 포인트 차감
        customer.setCustomerPoint(
                customer.getCustomerPoint() - orderPrice
        );

        // 같은 상품을 이미 주문했다면 기존 수량 증가
        OrderItem orderItem =
                customerProductRepository
                        .findByCustomerAndProduct(
                                customer,
                                product
                        )
                        .orElseGet(() ->
                                new OrderItem(
                                        customer,
                                        product,
                                        0
                                )
                        );

        orderItem.addQuantity(quantity);

        customerRepository.save(customer);
        customerProductRepository.save(orderItem);

        return findById(customerId);
    }

    // 주문 취소
    @Transactional
    public OrderListDto cancelOrder(
            String customerId,
            Long productId,
            int quantity
    ) {
        validateOrderRequest(productId, quantity);

        Customer customer = getCustomer(customerId);
        Product product = getProduct(productId);

        OrderItem orderItem =
                customerProductRepository
                        .findByCustomerAndProduct(
                                customer,
                                product
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.ORDER_NOT_FOUND
                                )
                        );

        if (orderItem.getQuantity() < quantity) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_QUANTITY
            );
        }

        // 수량 감소
        orderItem.decreaseQuantity(quantity);

        // 취소 금액만큼 포인트 환급
        double refundPrice =
                product.getProductPrice() * quantity;

        customer.setCustomerPoint(
                customer.getCustomerPoint() + refundPrice
        );

        // 남은 수량이 0이면 OrderItem 삭제
        if (orderItem.getQuantity() == 0) {
            customerProductRepository.delete(orderItem);
        } else {
            customerProductRepository.save(orderItem);
        }

        customerRepository.save(customer);

        return findById(customerId);
    }

    // 고객 조회 공통 메서드
    private Customer getCustomer(String customerId) {
        if (customerId == null
                || customerId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CUSTOMER_NOT_FOUND
                        )
                );
    }

    // 상품 조회 공통 메서드
    private Product getProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );
    }

    // 고객 생성 입력값 검증
    private void validateCustomerForCreate(
            Customer customer
    ) {
        if (customer == null
                || customer.getCustomerId() == null
                || customer.getCustomerId().isBlank()
                || customer.getCustomerPassword() == null
                || customer.getCustomerPassword().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }

    // 주문 입력값 검증
    private void validateOrderRequest(
            Long productId,
            int quantity
    ) {
        if (productId == null || quantity <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }

    // OrderItem 엔티티를 DTO로 변환
    private OrderItemDto convertToOrderItemDto(
            OrderItem orderItem
    ) {
        Product product = orderItem.getProduct();

        return OrderItemDto.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productPrice(product.getProductPrice())
                .quantity(orderItem.getQuantity())
                .build();
    }
}