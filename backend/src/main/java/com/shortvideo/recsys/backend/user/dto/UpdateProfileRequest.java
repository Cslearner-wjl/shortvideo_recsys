// 用户信息更新请求 DTO，覆盖昵称/联系方式/头像/简介等字段。
package com.shortvideo.recsys.backend.user.dto;

/**
 * 用户信息更新请求
 */
public record UpdateProfileRequest(
        String username,
        String phone,
        String email,
        String avatarUrl,
        String bio
) {
}
