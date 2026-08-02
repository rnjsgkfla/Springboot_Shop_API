package com.skala.shop.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    PRODUCT_DUPLICATED(HttpStatus.CONFLICT, "PRODUCT_DUPLICATED", "이미 등록된 상품명입니다."),
    PRODUCT_IN_USE(HttpStatus.CONFLICT, "PRODUCT_IN_USE", "주문 또는 찜에 사용 중인 상품은 삭제할 수 없습니다."),
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다."),
    DUPLICATE_CUSTOMER_ID(HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER_ID", "이미 사용 중인 고객 ID 입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "고객 ID 또는 비밀번호가 올바르지 않습니다."),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "로그인이 필요합니다."),
    INVALID_REFERRER(HttpStatus.BAD_REQUEST, "INVALID_REFERRER", "추천인 ID를 확인해 주세요."),
    INSUFFICIENT_FUNDS(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "보유 포인트가 부족합니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "상품 재고가 부족합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문한 상품을 찾을 수 없습니다."),
    WISH_DUPLICATED(HttpStatus.CONFLICT, "WISH_DUPLICATED", "이미 찜한 상품입니다."),
    WISH_NOT_FOUND(HttpStatus.NOT_FOUND, "WISH_NOT_FOUND", "찜한 상품을 찾을 수 없습니다."),
    INSUFFICIENT_QUANTITY(HttpStatus.CONFLICT, "INSUFFICIENT_QUANTITY", "취소 수량이 주문 수량보다 많습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값을 확인해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
