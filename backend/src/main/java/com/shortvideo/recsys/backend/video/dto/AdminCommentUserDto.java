package com.shortvideo.recsys.backend.video.dto;

public record AdminCommentUserDto(
        long id,
        String username,
        String phone,
        String email,
        int status
) {
}
