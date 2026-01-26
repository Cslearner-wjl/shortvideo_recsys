// 管理端用户信息 DTO。
package com.shortvideo.recsys.backend.admin.dto;

public record AdminUserDto(
        long id,
        String username,
        String phone,
        String email,
        int status,
        String createdAt
) {
}

