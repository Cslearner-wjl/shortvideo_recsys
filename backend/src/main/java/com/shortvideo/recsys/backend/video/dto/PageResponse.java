package com.shortvideo.recsys.backend.video.dto;

import java.util.List;

public record PageResponse<T>(
        long total,
        long page,
        long pageSize,
        List<T> items
) {
}

