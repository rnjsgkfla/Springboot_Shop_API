package com.skala.shop.service;

import com.skala.shop.dto.CustomerRegistrationRequest;
import com.skala.shop.dto.CustomerResponse;
import com.skala.shop.dto.LoginRequest;
import com.skala.shop.dto.LoginResponse;
import com.skala.shop.dto.OrderItemDto;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.dto.OrderRequest;
import com.skala.shop.entity.Customer;
import com.skala.shop.entity.OrderItem;
import com.skala.shop.entity.Product;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.CustomerProductRepository;
import com.skala.shop.repository.CustomerRepository;
import com.skala.shop.repository.ProductRepository;
import com.skala.shop.repository.WishRepository;
import com.skala.shop.security.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private static final double INITIAL_POINT = 1_000_000.0;
    private static final double REFERRAL_REWARD = 10_000.0;

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CustomerProductRepository customerProductRepository;
    private final WishRepository wishRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
            .map(CustomerResponse::from)
            .toList();
    }

    public OrderListDto findById(String customerId) {
        Customer customer = getCustomer(customerId);
        List<OrderItemDto> products = customerProductRepository
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

    @Transactional
    public CustomerResponse create(CustomerRegistrationRequest request) {
        if (customerRepository.existsById(request.customerId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CUSTOMER_ID);
        }

        Customer referrer = findReferrer(request);
        double initialPoint = INITIAL_POINT;
        if (referrer != null) {
            initialPoint += REFERRAL_REWARD;
            referrer.setCustomerPoint(
                referrer.getCustomerPoint() + REFERRAL_REWARD
            );
        }

        Customer customer = new Customer(
            request.customerId(),
            passwordEncoder.encode(request.customerPassword()),
            initialPoint
        );
        customer.setReferrer(referrer);

        return CustomerResponse.from(customerRepository.save(customer));
    }

    public LoginResponse login(LoginRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
            .orElseThrow(() ->
                new BusinessException(ErrorCode.INVALID_CREDENTIALS)
            );

        if (!passwordEncoder.matches(
                request.customerPassword(),
                customer.getCustomerPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return new LoginResponse(
            jwtTokenProvider.createToken(customer.getCustomerId()),
            "Bearer",
            jwtTokenProvider.getExpirationMinutes()
        );
    }

    @Transactional
    public CustomerResponse update(String customerId, Double customerPoint) {
        if (customerPoint == null || customerPoint < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Customer customer = getCustomer(customerId);
        customer.setCustomerPoint(customerPoint);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(String customerId) {
        Customer customer = getCustomer(customerId);

        List<Customer> referredCustomers = customerRepository.findByReferrer(customer);
        referredCustomers.forEach(referredCustomer -> referredCustomer.setReferrer(null));
        wishRepository.deleteByCustomer(customer);
        customerProductRepository.deleteAll(
            customerProductRepository.findByCustomerCustomerId(customerId)
        );
        customerRepository.delete(customer);
    }

    @Transactional
    public OrderListDto placeOrder(String customerId, OrderRequest request) {
        Customer customer = getCustomer(customerId);
        Product product = getProduct(request.productId());

        if (product.getStockQuantity() < request.quantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        double orderPrice = product.getProductPrice() * request.quantity();
        if (customer.getCustomerPoint() < orderPrice) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        customer.setCustomerPoint(customer.getCustomerPoint() - orderPrice);
        product.decreaseStock(request.quantity());

        OrderItem orderItem = customerProductRepository
            .findByCustomerAndProduct(customer, product)
            .orElseGet(() -> new OrderItem(customer, product, 0));
        orderItem.addQuantity(request.quantity());
        customerProductRepository.save(orderItem);

        return findById(customerId);
    }

    @Transactional
    public OrderListDto cancelOrder(String customerId, OrderRequest request) {
        Customer customer = getCustomer(customerId);
        Product product = getProduct(request.productId());
        OrderItem orderItem = customerProductRepository
            .findByCustomerAndProduct(customer, product)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.ORDER_NOT_FOUND)
            );

        if (orderItem.getQuantity() < request.quantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY);
        }

        orderItem.decreaseQuantity(request.quantity());
        customer.setCustomerPoint(
            customer.getCustomerPoint()
                + product.getProductPrice() * request.quantity()
        );
        product.increaseStock(request.quantity());

        if (orderItem.getQuantity() == 0) {
            customerProductRepository.delete(orderItem);
        }

        return findById(customerId);
    }

    private Customer findReferrer(CustomerRegistrationRequest request) {
        String referrerId = request.referrerId();
        if (referrerId == null || referrerId.isBlank()) {
            return null;
        }
        if (referrerId.equals(request.customerId())) {
            throw new BusinessException(ErrorCode.INVALID_REFERRER);
        }
        return customerRepository.findById(referrerId)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.INVALID_REFERRER)
            );
    }

    private Customer getCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return customerRepository.findById(customerId)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND)
            );
    }

    private Product getProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return productRepository.findById(productId)
            .orElseThrow(() ->
                new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );
    }

    private OrderItemDto convertToOrderItemDto(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        return OrderItemDto.builder()
            .productId(product.getId())
            .productName(product.getProductName())
            .productPrice(product.getProductPrice())
            .quantity(orderItem.getQuantity())
            .build();
    }
}
