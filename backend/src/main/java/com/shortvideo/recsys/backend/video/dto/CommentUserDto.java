// 评论用户信息 DTO，供评论列表展示使用。
package com.shortvideo.recsys.backend.video.dto;

public record CommentUserDto(
        long id,
        String username,
        String avatarUrl
) {
}
