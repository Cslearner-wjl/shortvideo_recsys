package com.shortvideo.recsys.backend.recommendation.dto;

import java.util.List;

public record RecommendationResponse<T>(
        long page,
        long pageSize,
        String nextCursor,
        List<T> items
) {
}

