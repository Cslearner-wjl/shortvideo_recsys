// 视频基础信息与统计数据 DTO，供用户端与管理端列表/详情展示。
package com.shortvideo.recsys.backend.video.dto;

public record VideoDto(
        long id,
        String title,
        String description,
        long uploaderUserId,
        String uploaderName,
        String videoUrl,
        String coverUrl,
        String auditStatus,
        boolean isHot,
        String createdAt,
        long playCount,
        long likeCount,
        long commentCount,
        long favoriteCount,
        boolean liked,
        boolean favorited,
        double hotScore
) {
}
