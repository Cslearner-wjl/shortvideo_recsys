package com.shortvideo.recsys.backend.admin.dto;

/**
 * 创建/更新管理员请求
 */
public record AdminAccountRequest(
        String username,
        String password
) {
}
