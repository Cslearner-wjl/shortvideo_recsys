package com.shortvideo.recsys.backend.rank.dto;

public record HotRankVideoDto(
        long id,
        String title,
        String description,
        long uploaderUserId,
        String videoUrl,
        String auditStatus,
        boolean isHot,
        String createdAt,
        long playCount,
        long likeCount,
        long commentCount,
        long favoriteCount,
        double hotScore
) {
}

