// 视频服务，负责视频存储、审核与用户端列表展示。
package com.shortvideo.recsys.backend.video;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.storage.MinioStorageService;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import com.shortvideo.recsys.backend.video.dto.VideoDto;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile({"docker", "test"})
public class VideoService {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VideoMapper videoMapper;
    private final VideoStatsMapper videoStatsMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final VideoFavoriteMapper videoFavoriteMapper;
    private final MinioStorageService minioStorageService;
    private final UserMapper userMapper;

    public VideoService(
            VideoMapper videoMapper,
            VideoStatsMapper videoStatsMapper,
            VideoLikeMapper videoLikeMapper,
            VideoFavoriteMapper videoFavoriteMapper,
            MinioStorageService minioStorageService,
            UserMapper userMapper
    ) {
        this.videoMapper = videoMapper;
        this.videoStatsMapper = videoStatsMapper;
        this.videoLikeMapper = videoLikeMapper;
        this.videoFavoriteMapper = videoFavoriteMapper;
        this.minioStorageService = minioStorageService;
        this.userMapper = userMapper;
    }

    @Transactional
    public VideoDto uploadVideo(long uploaderUserId, String title, String description, String tagsJson, MultipartFile file) {
        if (uploaderUserId <= 0 || isBlank(title) || file == null || file.isEmpty()) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }

        String objectKey = "videos/" + uploaderUserId + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());

        try (InputStream in = file.getInputStream()) {
            minioStorageService.putObject(objectKey, file.getContentType(), in, file.getSize());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCodes.MINIO_UNAVAILABLE, "对象存储不可用");
        }

        try {
            VideoEntity entity = new VideoEntity();
            entity.setUploaderUserId(uploaderUserId);
            entity.setTitle(title.trim());
            entity.setDescription(description);
            entity.setTags(tagsJson);
            entity.setVideoUrl(objectKey);
            entity.setAuditStatus("PENDING");
            entity.setIsHot(0);
            videoMapper.insert(entity);

            VideoStatsEntity stats = new VideoStatsEntity();
            stats.setVideoId(entity.getId());
            stats.setPlayCount(0L);
            stats.setLikeCount(0L);
            stats.setCommentCount(0L);
            stats.setFavoriteCount(0L);
            stats.setHotScore(0.0);
            videoStatsMapper.insert(stats);

            UserEntity uploader = userMapper.selectById(uploaderUserId);
            return toDto(entity, uploader, stats, false, false);
        } catch (Exception e) {
            try {
                minioStorageService.removeObject(objectKey);
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    public VideoDto getApprovedVideo(long id, Long viewerId) {
        VideoEntity entity = videoMapper.selectOne(new QueryWrapper<VideoEntity>()
                .eq("id", id)
                .eq("audit_status", "APPROVED")
                .last("limit 1"));
        if (entity == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
        VideoStatsEntity stats = videoStatsMapper.selectById(entity.getId());
        UserEntity uploader = userMapper.selectById(entity.getUploaderUserId());
        Set<Long> likedIds = loadLikedVideoIds(viewerId, List.of(entity.getId()));
        Set<Long> favoritedIds = loadFavoritedVideoIds(viewerId, List.of(entity.getId()));
        boolean liked = likedIds.contains(entity.getId());
        boolean favorited = favoritedIds.contains(entity.getId());
        return toDto(entity, uploader, stats, liked, favorited);
    }

    public PageResponse<VideoDto> pageApproved(String sort, long page, long pageSize, Long viewerId) {
        long safePage = page <= 0 ? 1 : page;
        long safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);

        QueryWrapper<VideoEntity> qw = new QueryWrapper<VideoEntity>().eq("audit_status", "APPROVED");
        if (Objects.equals(sort, "hot")) {
            qw.orderByDesc("is_hot").orderByDesc("created_at");
        } else {
            qw.orderByDesc("created_at");
        }

        Page<VideoEntity> p = videoMapper.selectPage(new Page<>(safePage, safeSize), qw);
        List<VideoEntity> videos = p.getRecords();
        List<Long> videoIds = videos.stream()
                .map(VideoEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> uploaderIds = videos.stream()
                .map(VideoEntity::getUploaderUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<VideoStatsEntity> statsList = videoIds.isEmpty() ? List.of() : videoStatsMapper.selectBatchIds(videoIds);
        List<UserEntity> uploaderList = uploaderIds.isEmpty() ? List.of() : userMapper.selectBatchIds(uploaderIds);

        Map<Long, VideoStatsEntity> statsMap = statsList.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(VideoStatsEntity::getVideoId, v -> v, (a, b) -> a));
        Map<Long, UserEntity> uploaderMap = uploaderList.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));

        Set<Long> likedIds = loadLikedVideoIds(viewerId, videoIds);
        Set<Long> favoritedIds = loadFavoritedVideoIds(viewerId, videoIds);

        List<VideoDto> items = videos.stream()
                .map(video -> toDto(
                        video,
                        uploaderMap.get(video.getUploaderUserId()),
                        statsMap.get(video.getId()),
                        likedIds.contains(video.getId()),
                        favoritedIds.contains(video.getId())
                ))
                .toList();
        return new PageResponse<>(p.getTotal(), safePage, safeSize, items);
    }

    @Transactional
    public void audit(long id, String status) {
        if (!Objects.equals(status, "APPROVED") && !Objects.equals(status, "REJECTED")) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        VideoEntity entity = videoMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
        entity.setAuditStatus(status);
        videoMapper.updateById(entity);
    }

    @Transactional
    public void setHot(long id, boolean isHot) {
        VideoEntity entity = videoMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
        entity.setIsHot(isHot ? 1 : 0);
        videoMapper.updateById(entity);
    }

    @Transactional
    public void deleteVideo(long id) {
        VideoEntity entity = videoMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
        String objectKey = entity.getVideoUrl();
        if (!isBlank(objectKey)) {
            minioStorageService.removeObject(objectKey);
        }
        videoMapper.deleteById(id);
    }

    private VideoDto toDto(VideoEntity entity, UserEntity uploader, VideoStatsEntity stats, boolean liked, boolean favorited) {
        String url = minioStorageService.toPublicUrl(entity.getVideoUrl());
        String coverUrl = null;
        if (!isBlank(entity.getCoverUrl())) {
            coverUrl = entity.getCoverUrl().startsWith("http") ? entity.getCoverUrl() : minioStorageService.toPublicUrl(entity.getCoverUrl());
        }
        String uploaderName = uploader == null ? null : uploader.getUsername();
        long playCount = stats == null || stats.getPlayCount() == null ? 0L : stats.getPlayCount();
        long likeCount = stats == null || stats.getLikeCount() == null ? 0L : stats.getLikeCount();
        long commentCount = stats == null || stats.getCommentCount() == null ? 0L : stats.getCommentCount();
        long favoriteCount = stats == null || stats.getFavoriteCount() == null ? 0L : stats.getFavoriteCount();
        double hotScore = stats == null || stats.getHotScore() == null ? 0.0 : stats.getHotScore();
        return new VideoDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getUploaderUserId() == null ? 0 : entity.getUploaderUserId(),
                uploaderName,
                url,
                coverUrl,
                entity.getAuditStatus(),
                entity.getIsHot() != null && entity.getIsHot() == 1,
                entity.getCreatedAt() == null ? null : dtf.format(entity.getCreatedAt()),
                playCount,
                likeCount,
                commentCount,
                favoriteCount,
                liked,
                favorited,
                hotScore
        );
    }

    private Set<Long> loadLikedVideoIds(Long viewerId, List<Long> videoIds) {
        if (viewerId == null || viewerId <= 0 || videoIds == null || videoIds.isEmpty()) {
            return Collections.emptySet();
        }
        return videoLikeMapper.selectList(new QueryWrapper<VideoLikeEntity>()
                        .eq("user_id", viewerId)
                        .in("video_id", videoIds))
                .stream()
                .map(VideoLikeEntity::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> loadFavoritedVideoIds(Long viewerId, List<Long> videoIds) {
        if (viewerId == null || viewerId <= 0 || videoIds == null || videoIds.isEmpty()) {
            return Collections.emptySet();
        }
        return videoFavoriteMapper.selectList(new QueryWrapper<VideoFavoriteEntity>()
                        .eq("user_id", viewerId)
                        .in("video_id", videoIds))
                .stream()
                .map(VideoFavoriteEntity::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String sanitizeFilename(String name) {
        if (isBlank(name)) {
            return "video.bin";
        }
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
