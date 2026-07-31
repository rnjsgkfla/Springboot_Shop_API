package com.skala.shop.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        // 업무 규칙 위반은 정상적인 흐름의 하나이므로 WARN 레벨로 남깁니다.
        log.warn("BusinessException: {} {}", errorCode.getCode(), request.getRequestURI());
        return ResponseEntity.status(errorCode.getStatus())
                .body(createResponse(errorCode, errorCode.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        // 필드 오류의 순서는 실행마다 달라질 수 있으므로,
        // "필드명: 사유" 형태로 모두 모아 필드명 순으로 정렬해 항상 같은 메시지를 만듭니다.
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = ErrorCode.INVALID_REQUEST.getMessage();
        }
        return ResponseEntity.badRequest()
                .body(createResponse(ErrorCode.INVALID_REQUEST, message, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(createResponse(
                        ErrorCode.INVALID_REQUEST,
                        "JSON 형식과 필드 값을 확인해 주세요.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        // 예상하지 못한 오류는 원인을 추적할 수 있도록 반드시 스택 트레이스와 함께 기록합니다.
        // 이 로그가 없으면 500 오류의 원인을 찾을 방법이 없습니다.
        log.error("Unexpected exception at {}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createResponse(
                        ErrorCode.INTERNAL_ERROR,
                        ErrorCode.INTERNAL_ERROR.getMessage(),
                        request.getRequestURI()
                ));
    }

    private ErrorResponse createResponse(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
            LocalDateTime.now(),
            errorCode.getStatus().value(),
            errorCode.getCode(),
            message,
            path
        );
    }
}