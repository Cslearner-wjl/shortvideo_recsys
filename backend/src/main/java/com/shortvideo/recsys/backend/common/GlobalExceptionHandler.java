package com.shortvideo.recsys.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getCode() == ErrorCodes.UNAUTHORIZED || ex.getCode() == ErrorCodes.ACCOUNT_OR_PASSWORD_INCORRECT) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex.getCode() == ErrorCodes.ACCOUNT_FROZEN) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex.getCode() == ErrorCodes.RESOURCE_NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getCode() >= 40900 && ex.getCode() < 41000) {
            status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (request != null) {
            log.warn("Bad request body for {} {}", request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.warn("Bad request body", ex);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCodes.BAD_REQUEST, "请求体不是合法 JSON"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex, HttpServletRequest request) {
        if (request != null) {
            log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.error("Unhandled exception", ex);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "服务内部错误"));
    }
}
