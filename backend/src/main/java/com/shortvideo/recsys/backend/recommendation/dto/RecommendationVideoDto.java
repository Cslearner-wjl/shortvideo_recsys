package com.shortvideo.recsys.backend.recommendation.dto;

import java.util.List;

public record RecommendationVideoDto(
        long id,
        String title,
        String description,
        long uploaderUserId,
        String videoUrl,
        String auditStatus,
        boolean isHot,
        String createdAt,
        List<String> tags,
        long playCount,
        long likeCount,
        long commentCount,
        long favoriteCount,
        boolean liked,
        boolean favorited,
        double hotScore
) {
}
