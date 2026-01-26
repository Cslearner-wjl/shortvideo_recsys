package com.shortvideo.recsys.backend.common;

/**
 * 业务异常（统一由 GlobalExceptionHandler 转换为 ApiResponse）。
 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

