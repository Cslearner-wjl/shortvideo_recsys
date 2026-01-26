package com.shortvideo.recsys.backend.user.dto;

/**
 * 修改密码请求
 */
public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
