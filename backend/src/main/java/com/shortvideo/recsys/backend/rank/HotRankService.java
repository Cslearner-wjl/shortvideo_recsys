package com.shortvideo.recsys.backend.rank;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shortvideo.recsys.backend.storage.MinioStorageService;
import com.shortvideo.recsys.backend.video.VideoEntity;
import com.shortvideo.recsys.backend.video.VideoMapper;
import com.shortvideo.recsys.backend.video.VideoStatsEntity;
import com.shortvideo.recsys.backend.video.VideoStatsMapper;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import com.shortvideo.recsys.backend.rank.dto.HotRankVideoDto;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"docker", "test"})
public class HotRankService {
    private static final Logger log = LoggerFactory.getLogger(HotRankService.class);
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HotRankProperties properties;
    private final HotScoreCalculator calculator;
    private final HotRankCache cache;
    private final VideoMapper videoMapper;
    private final VideoStatsMapper videoStatsMapper;
    private final MinioStorageService minioStorageService;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public HotRankService(
            HotRankProperties properties,
            HotScoreCalculator calculator,
            HotRankCache cache,
            VideoMapper videoMapper,
            VideoStatsMapper videoStatsMapper,
            MinioStorageService minioStorageService,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.properties = properties;
        this.calculator = calculator;
        this.cache = cache;
        this.videoMapper = videoMapper;
        this.videoStatsMapper = videoStatsMapper;
        this.minioStorageService = minioStorageService;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Transactional
    public void refresh() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.isManagedByStreaming()) {
            return;
        }

        List<VideoEntity> approvedVideos = videoMapper.selectList(
                new QueryWrapper<VideoEntity>().select("id").eq("audit_status", "APPROVED"));
        if (approvedVideos == null || approvedVideos.isEmpty()) {
            cache.replaceTop(List.of());
            return;
        }

        List<Long> ids = approvedVideos.stream()
                .map(VideoEntity::getId)
                .filter(id -> id != null && id > 0)
                .toList();

        Map<Long, VideoStatsEntity> statsById = new HashMap<>();
        for (VideoStatsEntity stats : videoStatsMapper.selectBatchIds(ids)) {
            if (stats != null && stats.getVideoId() != null) {
                statsById.put(stats.getVideoId(), stats);
            }
        }

        List<HotRankEntry> entries = new ArrayList<>(ids.size());
        for (Long id : ids) {
            VideoStatsEntity stats = statsById.get(id);
            if (stats == null) {
                stats = ensureStatsRow(id);
            }

            double score = calculator.compute(
                    properties.getWeights(),
                    safe(stats.getPlayCount()),
                    safe(stats.getLikeCount()),
                    safe(stats.getCommentCount()),
                    safe(stats.getFavoriteCount())
            );

            videoStatsMapper.updateHotScore(id, score);
            entries.add(new HotRankEntry(id, score));
        }

        entries.sort(Comparator
                .comparingDouble(HotRankEntry::score).reversed()
                .thenComparing(Comparator.comparingLong(HotRankEntry::videoId).reversed()));

        int topn = Math.max(1, properties.getTopn());
        List<HotRankEntry> top = entries.size() > topn ? entries.subList(0, topn) : entries;
        try {
            cache.replaceTop(top);
        } catch (Exception e) {
            log.warn("热门榜单缓存刷新失败，将仅依赖数据库 hot_score 兜底。", e);
        }
    }

    public PageResponse<HotRankVideoDto> page(long page, long pageSize) {
        long safePage = page <= 0 ? 1 : page;
        long safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);

        if (properties.isEnabled() && !properties.isManagedByStreaming() && cache.size() == 0) {
            refresh();
        }

        long total = cache.size();
        long start = (safePage - 1) * safeSize;
        long end = start + safeSize - 1;

        List<Long> ids = cache.getIds(start, end);
        if (ids.isEmpty()) {
            return new PageResponse<>(0, safePage, safeSize, List.of());
        }

        Map<Long, VideoEntity> videosById = new HashMap<>();
        for (VideoEntity v : videoMapper.selectBatchIds(ids)) {
            if (v != null && v.getId() != null) {
                videosById.put(v.getId(), v);
            }
        }

        Map<Long, VideoStatsEntity> statsById = new HashMap<>();
        for (VideoStatsEntity s : videoStatsMapper.selectBatchIds(ids)) {
            if (s != null && s.getVideoId() != null) {
                statsById.put(s.getVideoId(), s);
            }
        }

        Map<Long, VideoStatsEntity> redisStatsById = loadStatsFromRedis(ids);

        List<HotRankVideoDto> items = ids.stream()
                .map(videosById::get)
                .filter(v -> v != null && "APPROVED".equals(v.getAuditStatus()))
                .map(v -> {
                    VideoStatsEntity redisStats = redisStatsById.get(v.getId());
                    VideoStatsEntity dbStats = statsById.get(v.getId());
                    return toDto(v, redisStats != null ? redisStats : dbStats);
                })
                .toList();

        return new PageResponse<>(total, safePage, safeSize, items);
    }

    private HotRankVideoDto toDto(VideoEntity video, VideoStatsEntity stats) {
        VideoStatsEntity safeStats = stats == null ? new VideoStatsEntity() : stats;
        String url = minioStorageService.toPublicUrl(video.getVideoUrl());
        return new HotRankVideoDto(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUploaderUserId() == null ? 0 : video.getUploaderUserId(),
                url,
                video.getAuditStatus(),
                video.getIsHot() != null && video.getIsHot() == 1,
                video.getCreatedAt() == null ? null : dtf.format(video.getCreatedAt()),
                safe(safeStats.getPlayCount()),
                safe(safeStats.getLikeCount()),
                safe(safeStats.getCommentCount()),
                safe(safeStats.getFavoriteCount()),
                safeDouble(safeStats.getHotScore())
        );
    }

    private VideoStatsEntity ensureStatsRow(long videoId) {
        VideoStatsEntity existing = videoStatsMapper.selectById(videoId);
        if (existing != null) {
            return existing;
        }
        try {
            VideoStatsEntity stats = new VideoStatsEntity();
            stats.setVideoId(videoId);
            stats.setPlayCount(0L);
            stats.setLikeCount(0L);
            stats.setCommentCount(0L);
            stats.setFavoriteCount(0L);
            stats.setHotScore(0.0);
            videoStatsMapper.insert(stats);
            return stats;
        } catch (DuplicateKeyException ignored) {
            VideoStatsEntity existing2 = videoStatsMapper.selectById(videoId);
            return existing2 == null ? new VideoStatsEntity() : existing2;
        }
    }

    private static long safe(Long v) {
        return v == null ? 0L : Math.max(0L, v);
    }

    private static double safeDouble(Double v) {
        return v == null ? 0.0 : v;
    }

    private Map<Long, VideoStatsEntity> loadStatsFromRedis(List<Long> ids) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null || ids == null || ids.isEmpty()) {
            return Map.of();
        }

        final RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
        final byte[] fPlay = serializer.serialize("play_count");
        final byte[] fLike = serializer.serialize("like_count");
        final byte[] fComment = serializer.serialize("comment_count");
        final byte[] fFavorite = serializer.serialize("favorite_count");
        final byte[] fHot = serializer.serialize("hot_score");

        List<Object> pipelined = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long id : ids) {
                if (id == null || id <= 0) {
                    continue;
                }
                String key = properties.getStatsHashPrefix() + id;
                byte[] keyBytes = serializer.serialize(key);
                connection.hashCommands().hMGet(keyBytes, fPlay, fLike, fComment, fFavorite, fHot);
            }
            return null;
        });

        if (pipelined == null || pipelined.isEmpty()) {
            return Map.of();
        }

        Map<Long, VideoStatsEntity> result = new HashMap<>();
        int i = 0;
        for (Long id : ids) {
            if (id == null || id <= 0) {
                continue;
            }
            if (i >= pipelined.size()) {
                break;
            }
            Object item = pipelined.get(i++);
            if (!(item instanceof List<?> values) || values.isEmpty()) {
                continue;
            }

            Long play = parseLong(bytesAt(values, 0));
            Long like = parseLong(bytesAt(values, 1));
            Long comment = parseLong(bytesAt(values, 2));
            Long favorite = parseLong(bytesAt(values, 3));
            Double hot = parseDouble(bytesAt(values, 4));

            if (play == null && like == null && comment == null && favorite == null && hot == null) {
                continue;
            }

            VideoStatsEntity stats = new VideoStatsEntity();
            stats.setVideoId(id);
            stats.setPlayCount(play);
            stats.setLikeCount(like);
            stats.setCommentCount(comment);
            stats.setFavoriteCount(favorite);
            stats.setHotScore(hot);
            result.put(id, stats);
        }

        return result;
    }

    private static byte[] bytesAt(List<?> values, int index) {
        if (index < 0 || index >= values.size()) {
            return null;
        }
        Object v = values.get(index);
        if (v == null) {
            return null;
        }
        if (v instanceof byte[] b) {
            return b;
        }
        if (v instanceof String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
        return String.valueOf(v).getBytes(StandardCharsets.UTF_8);
    }

    private static Long parseLong(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return Long.parseLong(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double parseDouble(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return Double.parseDouble(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }
}
