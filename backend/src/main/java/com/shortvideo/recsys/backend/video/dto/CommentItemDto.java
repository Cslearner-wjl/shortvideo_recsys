package com.shortvideo.recsys.backend.video.dto;

public record CommentItemDto(
        long id,
        String content,
        String createdAt,
        long likeCount,
        boolean liked,
        CommentUserDto user
) {
}
