package com.skala.shop.controller;

import com.skala.shop.dto.OrderItemDto;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.entity.Customer;
import com.skala.shop.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // 전체 고객 목록 조회
    @GetMapping
    public ResponseEntity<List<Customer>> findAll() {
        List<Customer> customers =
                customerService.findAll()
                        .stream()
                        .map(this::withoutPassword)
                        .toList();

        return ResponseEntity.ok(customers);
    }

    // 고객 정보와 주문상품 목록 조회
    @GetMapping("/{customerId}")
    public ResponseEntity<OrderListDto> findById(
            @PathVariable String customerId
    ) {
        return ResponseEntity.ok(
                customerService.findById(customerId)
        );
    }

    // 고객 등록
    @PostMapping
    public ResponseEntity<Customer> create(
            @RequestBody Customer customer
    ) {
        Customer savedCustomer =
                customerService.create(customer);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(withoutPassword(savedCustomer));
    }

    // 고객 로그인
    @PostMapping("/login")
    public ResponseEntity<Customer> login(
            @RequestBody Customer customer
    ) {
        Customer loggedInCustomer =
                customerService.login(
                        customer.getCustomerId(),
                        customer.getCustomerPassword()
                );

        return ResponseEntity.ok(
                withoutPassword(loggedInCustomer)
        );
    }

    // 고객 포인트 수정
    @PutMapping
    public ResponseEntity<Customer> update(
            @RequestBody Customer customer
    ) {
        Customer updatedCustomer =
                customerService.update(customer);

        return ResponseEntity.ok(
                withoutPassword(updatedCustomer)
        );
    }

    // 고객 삭제
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> delete(
            @PathVariable String customerId
    ) {
        customerService.delete(customerId);

        return ResponseEntity.noContent().build();
    }

    // 상품 주문
    @PostMapping("/{customerId}/order")
    public ResponseEntity<OrderListDto> placeOrder(
            @PathVariable String customerId,
            @RequestBody OrderItemDto orderRequest
    ) {
        OrderListDto orderList =
                customerService.placeOrder(
                        customerId,
                        orderRequest.getProductId(),
                        orderRequest.getQuantity()
                );

        return ResponseEntity.ok(orderList);
    }

    // 주문 취소
    @PostMapping("/{customerId}/cancel")
    public ResponseEntity<OrderListDto> cancelOrder(
            @PathVariable String customerId,
            @RequestBody OrderItemDto orderRequest
    ) {
        OrderListDto orderList =
                customerService.cancelOrder(
                        customerId,
                        orderRequest.getProductId(),
                        orderRequest.getQuantity()
                );

        return ResponseEntity.ok(orderList);
    }

    // API 응답에서 비밀번호를 제외하기 위한 변환
    private Customer withoutPassword(Customer customer) {
        return new Customer(
                customer.getCustomerId(),
                customer.getCustomerPoint()
        );
    }
}