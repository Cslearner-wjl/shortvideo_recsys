package com.shortvideo.recsys.backend.common;

/**
 * 错误码约定。
 */
public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final int BAD_REQUEST = 40000;

    public static final int USERNAME_EXISTS = 40901;
    public static final int EMAIL_EXISTS = 40902;
    public static final int PHONE_EXISTS = 40903;

    public static final int EMAIL_CODE_INVALID = 40010;

    public static final int UNAUTHORIZED = 40100;
    public static final int ACCOUNT_OR_PASSWORD_INCORRECT = 40101;

    public static final int ACCOUNT_FROZEN = 40301;

    public static final int RESOURCE_NOT_FOUND = 40401;
    public static final int MINIO_UNAVAILABLE = 50010;
}
