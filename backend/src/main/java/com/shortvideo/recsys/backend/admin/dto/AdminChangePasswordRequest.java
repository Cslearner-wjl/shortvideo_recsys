package com.shortvideo.recsys.backend.admin.dto;

/**
 * 管理员修改密码请求
 */
public record AdminChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
