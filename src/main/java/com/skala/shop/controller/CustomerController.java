package com.skala.shop.controller;

import com.skala.shop.dto.CustomerRegistrationRequest;
import com.skala.shop.dto.CustomerResponse;
import com.skala.shop.dto.LoginRequest;
import com.skala.shop.dto.LoginResponse;
import com.skala.shop.dto.OrderListDto;
import com.skala.shop.dto.OrderRequest;
import com.skala.shop.entity.Customer;
import com.skala.shop.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "고객", description = "회원가입, 로그인, 고객 정보와 주문 관리 API")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "고객 목록 조회", description = "등록된 고객 목록을 조회합니다. JWT 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CustomerResponse>> findAll() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인한 고객의 포인트와 주문 상품을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderListDto> findMe(Authentication authentication) {
        return ResponseEntity.ok(
            customerService.findById(authentication.getName())
        );
    }

    @PostMapping
    @Operation(summary = "회원가입", description = "고객을 등록합니다. 추천인 ID 입력 시 두 고객에게 각각 10,000포인트를 지급합니다.")
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(customerService.create(request));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "고객 ID와 비밀번호를 확인하고 JWT 액세스 토큰을 발급합니다.")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(customerService.login(request));
    }

    @PutMapping("/me")
    @Operation(summary = "내 포인트 수정", description = "로그인한 고객의 보유 포인트를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CustomerResponse> update(
            Authentication authentication,
            @RequestBody Customer customer
    ) {
        return ResponseEntity.ok(
            customerService.update(
                authentication.getName(),
                customer.getCustomerPoint()
            )
        );
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "로그인한 고객과 해당 고객의 주문 및 찜 정보를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(Authentication authentication) {
        customerService.delete(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/order")
    @Operation(summary = "상품 주문", description = "포인트와 재고를 확인한 뒤 상품을 주문합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderListDto> placeOrder(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(
            customerService.placeOrder(authentication.getName(), request)
        );
    }

    @PostMapping("/cancel")
    @Operation(summary = "주문 취소", description = "주문 수량을 줄이고 취소 금액과 재고를 복구합니다.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderListDto> cancelOrder(
            Authentication authentication,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(
            customerService.cancelOrder(authentication.getName(), request)
        );
    }
}
