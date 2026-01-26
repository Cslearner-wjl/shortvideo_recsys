// 用户认证响应数据，包含基础资料与状态字段。
package com.shortvideo.recsys.backend.auth.dto;

public record AuthUserDto(
        long id,
        String username,
        String phone,
        String email,
        String avatarUrl,
        String bio,
        int status
) {
}
