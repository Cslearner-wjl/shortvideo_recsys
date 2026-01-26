package com.shortvideo.recsys.backend.admin.dto;

/**
 * 管理员列表项 DTO
 */
public record AdminAccountDto(
        long id,
        String username,
        String createdAt
) {
}
