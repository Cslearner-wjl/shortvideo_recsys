package com.shortvideo.recsys.backend.auth;

/**
 * 已认证用户信息（放入 Spring Security Context）。
 */
public record AuthenticatedUser(long userId, String username, int status) {
    public boolean isFrozen() {
        return status == 0;
    }
}

