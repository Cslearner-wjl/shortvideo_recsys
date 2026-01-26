package com.shortvideo.recsys.backend.video.dto;

public record AdminCommentItemDto(
        long id,
        String content,
        String createdAt,
        long likeCount,
        boolean liked,
        AdminCommentUserDto user
) {
}
