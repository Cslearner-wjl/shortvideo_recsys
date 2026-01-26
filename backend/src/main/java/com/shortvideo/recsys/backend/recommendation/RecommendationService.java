package com.shortvideo.recsys.backend.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shortvideo.recsys.backend.rank.HotRankCache;
import com.shortvideo.recsys.backend.recommendation.dto.RecommendationResponse;
import com.shortvideo.recsys.backend.recommendation.dto.RecommendationVideoDto;
import com.shortvideo.recsys.backend.storage.MinioStorageService;
import com.shortvideo.recsys.backend.video.UserActionEntity;
import com.shortvideo.recsys.backend.video.UserActionMapper;
import com.shortvideo.recsys.backend.video.VideoFavoriteEntity;
import com.shortvideo.recsys.backend.video.VideoFavoriteMapper;
import com.shortvideo.recsys.backend.video.VideoEntity;
import com.shortvideo.recsys.backend.video.VideoLikeEntity;
import com.shortvideo.recsys.backend.video.VideoLikeMapper;
import com.shortvideo.recsys.backend.video.VideoMapper;
import com.shortvideo.recsys.backend.video.VideoStatsEntity;
import com.shortvideo.recsys.backend.video.VideoStatsMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "test"})
public class RecommendationService {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RecommendationProperties properties;
    private final VideoTagsParser tagsParser;
    private final UserActionMapper userActionMapper;
    private final VideoMapper videoMapper;
    private final VideoStatsMapper videoStatsMapper;
    private final VideoLikeMapper videoLikeMapper;
    private final VideoFavoriteMapper videoFavoriteMapper;
    private final MinioStorageService minioStorageService;
    private final HotRankCache hotRankCache;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public RecommendationService(
            RecommendationProperties properties,
            VideoTagsParser tagsParser,
            UserActionMapper userActionMapper,
            VideoMapper videoMapper,
            VideoStatsMapper videoStatsMapper,
            VideoLikeMapper videoLikeMapper,
            VideoFavoriteMapper videoFavoriteMapper,
            MinioStorageService minioStorageService,
            ObjectProvider<HotRankCache> hotRankCache,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.properties = properties;
        this.tagsParser = tagsParser;
        this.userActionMapper = userActionMapper;
        this.videoMapper = videoMapper;
        this.videoStatsMapper = videoStatsMapper;
        this.videoLikeMapper = videoLikeMapper;
        this.videoFavoriteMapper = videoFavoriteMapper;
        this.minioStorageService = minioStorageService;
        this.hotRankCache = hotRankCache.getIfAvailable();
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public RecommendationResponse<RecommendationVideoDto> recommend(long userId, Long page, Long pageSize, String cursor) {
        if (!properties.isEnabled()) {
            return new RecommendationResponse<>(1, 0, null, List.of());
        }

        long safePage = page == null || page <= 0 ? 1 : page;
        long safeSize = pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 100);

        long offset = parseCursor(cursor);
        if (cursor == null || cursor.trim().isEmpty()) {
            offset = (safePage - 1) * safeSize;
        } else {
            safePage = offset / safeSize + 1;
        }

        AlsPage alsPage = tryReadAlsPage(userId, offset, safeSize);
        if (alsPage != null) {
            List<Long> candidateIds = alsPage.ids();

            Map<Long, VideoEntity> videosById = toVideoMap(candidateIds);
            Map<Long, VideoStatsEntity> statsById = toStatsMap(candidateIds);

            Set<Long> likedIds = loadLikedVideoIds(userId, candidateIds);
            Set<Long> favoritedIds = loadFavoritedVideoIds(userId, candidateIds);
            List<RecommendationVideoDto> items = candidateIds.stream()
                    .map(videosById::get)
                    .filter(v -> v != null && "APPROVED".equals(v.getAuditStatus()))
                    .map(v -> toDto(
                            v,
                            statsById.get(v.getId()),
                            likedIds.contains(v.getId()),
                            favoritedIds.contains(v.getId())
                    ))
                    .toList();

            if (items.isEmpty()) {
                // Redis 中可能存在过期/无效/未通过审核的 videoId，避免返回空列表，回退到规则推荐。
                alsPage = null;
            } else {
            String nextCursor = alsPage.hasMore() ? String.valueOf(offset + safeSize) : null;
            return new RecommendationResponse<>(safePage, safeSize, nextCursor, items);
            }
        }

        List<UserActionEntity> recentActions = listRecentActions(userId, properties.getRecentActionsLimit());
        Set<Long> excludeVideoIds = recentActions.stream()
                .map(UserActionEntity::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        UserTagProfile profile = buildUserTagProfile(recentActions);
        List<Long> candidateIds = buildCandidates(userId, offset, safeSize, profile, excludeVideoIds);

        Map<Long, VideoEntity> videosById = toVideoMap(candidateIds);
        Map<Long, VideoStatsEntity> statsById = toStatsMap(candidateIds);

        Set<Long> likedIds = loadLikedVideoIds(userId, candidateIds);
        Set<Long> favoritedIds = loadFavoritedVideoIds(userId, candidateIds);
        List<RecommendationVideoDto> items = candidateIds.stream()
                .map(videosById::get)
                .filter(v -> v != null && "APPROVED".equals(v.getAuditStatus()))
                .map(v -> toDto(
                        v,
                        statsById.get(v.getId()),
                        likedIds.contains(v.getId()),
                        favoritedIds.contains(v.getId())
                ))
                .toList();

        String nextCursor = items.size() < safeSize ? null : String.valueOf(offset + safeSize);
        return new RecommendationResponse<>(safePage, safeSize, nextCursor, items);
    }

    private record AlsPage(List<Long> ids, boolean hasMore) {
    }

    private AlsPage tryReadAlsPage(long userId, long offset, long pageSize) {
        RecommendationProperties.Als als = properties.getAls();
        if (als == null || !als.isEnabled()) {
            return null;
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }

        String prefix = als.getRedisPrefix() == null ? "" : als.getRedisPrefix().trim();
        if (prefix.isEmpty()) {
            prefix = "rec:user:";
        }

        int topn = Math.max(1, als.getTopn());
        long safeOffset = Math.max(0L, offset);
        long safePageSize = Math.max(1L, Math.min(pageSize, 200L));
        if (safeOffset >= topn) {
            return new AlsPage(List.of(), false);
        }

        String key = prefix + userId;
        Long total = null;
        try {
            total = redisTemplate.opsForList().size(key);
        } catch (Exception ignored) {
        }

        if (total == null || total <= 0) {
            return null;
        }

        long end = Math.min((long) topn - 1L, safeOffset + safePageSize - 1L);
        List<String> raw;
        try {
            raw = redisTemplate.opsForList().range(key, safeOffset, end);
        } catch (Exception ignored) {
            return null;
        }
        if (raw == null || raw.isEmpty()) {
            return new AlsPage(List.of(), false);
        }

        List<Long> ids = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            try {
                long id = Long.parseLong(s.trim());
                if (id > 0) {
                    ids.add(id);
                }
            } catch (Exception ignored) {
            }
        }

        boolean hasMore = total > (safeOffset + safePageSize) && (safeOffset + safePageSize) < topn;
        return new AlsPage(ids, hasMore);
    }

    private List<Long> buildCandidates(
            long userId,
            long offset,
            long pageSize,
            UserTagProfile profile,
            Set<Long> exclude
    ) {
        int need = (int) pageSize;
        List<Long> result = new ArrayList<>(need);

        Random random = new Random(properties.getRandomSeed() ^ userId ^ offset);

        List<VideoEntity> pool = listApprovedCandidatePool(properties.getCandidatePoolSize());
        Map<Long, List<String>> tagsByVideoId = new HashMap<>();
        for (VideoEntity v : pool) {
            if (v == null || v.getId() == null) {
                continue;
            }
            tagsByVideoId.put(v.getId(), tagsParser.parseTags(v.getTags()));
        }

        if (profile == null || profile.isEmpty()) {
            List<Long> hot = listHotIds(need * 2L);
            appendUnique(result, hot, exclude, need);
            if (result.size() < need) {
                appendRandomFromPool(result, pool, exclude, random, need);
            }
            return result;
        }

        int tagQuota = clampQuota(need, properties.getTagRatio());
        int hotQuota = clampQuota(need, properties.getHotRatio());
        int randomQuota = Math.max(0, need - tagQuota - hotQuota);

        List<Long> tagSorted = rankByTagMatch(pool, tagsByVideoId, profile);
        appendUnique(result, tagSorted, exclude, tagQuota);

        List<Long> hot = listHotIds(hotQuota * 3L);
        appendUnique(result, hot, exclude, tagQuota + hotQuota);

        appendRandomFromPool(result, pool, exclude, random, tagQuota + hotQuota + randomQuota);

        if (result.size() < need) {
            appendUnique(result, tagSorted, exclude, need);
            appendUnique(result, hot, exclude, need);
            appendRandomFromPool(result, pool, exclude, random, need);
        }

        return result.size() > need ? result.subList(0, need) : result;
    }

    private UserTagProfile buildUserTagProfile(List<UserActionEntity> recentActions) {
        if (recentActions == null || recentActions.isEmpty()) {
            return new UserTagProfile(Map.of(), List.of());
        }

        Set<Long> videoIds = recentActions.stream()
                .map(UserActionEntity::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (videoIds.isEmpty()) {
            return new UserTagProfile(Map.of(), List.of());
        }

        Map<Long, VideoEntity> videosById = new HashMap<>();
        for (VideoEntity v : videoMapper.selectBatchIds(videoIds)) {
            if (v != null && v.getId() != null) {
                videosById.put(v.getId(), v);
            }
        }

        Map<String, Integer> scores = new HashMap<>();
        for (UserActionEntity action : recentActions) {
            if (action == null || action.getVideoId() == null) {
                continue;
            }
            VideoEntity v = videosById.get(action.getVideoId());
            if (v == null) {
                continue;
            }
            int w = actionWeight(action.getActionType());
            if (w <= 0) {
                continue;
            }
            for (String tag : tagsParser.parseTags(v.getTags())) {
                scores.merge(tag, w, Integer::sum);
            }
        }

        List<String> topTags = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(Math.max(0, properties.getTopTags()))
                .map(Map.Entry::getKey)
                .toList();

        return new UserTagProfile(scores, topTags);
    }

    private int actionWeight(String actionType) {
        if (actionType == null) {
            return 0;
        }
        RecommendationProperties.ActionWeights w = properties.getActionWeights();
        return switch (actionType) {
            case "FAVORITE" -> w.getFavorite();
            case "COMMENT" -> w.getComment();
            case "LIKE" -> w.getLike();
            case "PLAY" -> w.getPlay();
            default -> 0;
        };
    }

    private static int clampQuota(int total, double ratio) {
        double safe = Double.isFinite(ratio) ? ratio : 0.0;
        int q = (int) Math.round(total * safe);
        return Math.max(0, Math.min(total, q));
    }

    private List<Long> rankByTagMatch(List<VideoEntity> pool, Map<Long, List<String>> tagsByVideoId, UserTagProfile profile) {
        if (pool == null || pool.isEmpty() || profile == null || profile.isEmpty()) {
            return List.of();
        }
        Set<String> preferred = new HashSet<>(profile.topTags());

        List<TagCandidate> candidates = new ArrayList<>();
        for (VideoEntity v : pool) {
            if (v == null || v.getId() == null || !"APPROVED".equals(v.getAuditStatus())) {
                continue;
            }
            List<String> tags = tagsByVideoId.getOrDefault(v.getId(), List.of());
            int score = 0;
            for (String t : tags) {
                if (preferred.contains(t)) {
                    score += profile.tagScores().getOrDefault(t, 0);
                }
            }
            if (score > 0) {
                candidates.add(new TagCandidate(v.getId(), score));
            }
        }

        candidates.sort(Comparator.<TagCandidate>comparingInt(TagCandidate::score).reversed().thenComparingLong(TagCandidate::videoId).reversed());
        return candidates.stream().map(TagCandidate::videoId).toList();
    }

    private static class TagCandidate {
        private final long videoId;
        private final int score;

        private TagCandidate(long videoId, int score) {
            this.videoId = videoId;
            this.score = score;
        }

        public long videoId() {
            return videoId;
        }

        public int score() {
            return score;
        }
    }

    private void appendRandomFromPool(List<Long> out, List<VideoEntity> pool, Set<Long> exclude, Random random, int targetSize) {
        if (out.size() >= targetSize || pool == null || pool.isEmpty()) {
            return;
        }
        List<Long> ids = pool.stream()
                .map(VideoEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled, random);
        appendUnique(out, shuffled, exclude, targetSize);
    }

    private static void appendUnique(List<Long> out, List<Long> candidates, Set<Long> exclude, int targetSize) {
        if (out.size() >= targetSize || candidates == null || candidates.isEmpty()) {
            return;
        }
        Set<Long> exists = new HashSet<>(out);
        for (Long id : candidates) {
            if (out.size() >= targetSize) {
                return;
            }
            if (id == null || id <= 0) {
                continue;
            }
            if (exclude != null && exclude.contains(id)) {
                continue;
            }
            if (exists.add(id)) {
                out.add(id);
            }
        }
    }

    private List<UserActionEntity> listRecentActions(long userId, int limit) {
        int safeLimit = Math.max(0, Math.min(limit, 2000));
        if (safeLimit == 0) {
            return List.of();
        }
        return userActionMapper.selectList(new QueryWrapper<UserActionEntity>()
                .eq("user_id", userId)
                .orderByDesc("action_time")
                .last("limit " + safeLimit));
    }

    private List<VideoEntity> listApprovedCandidatePool(int limit) {
        int safeLimit = Math.max(0, Math.min(limit, 5000));
        if (safeLimit == 0) {
            return List.of();
        }
        return videoMapper.selectList(new QueryWrapper<VideoEntity>()
                .eq("audit_status", "APPROVED")
                .orderByDesc("created_at")
                .last("limit " + safeLimit));
    }

    private List<Long> listHotIds(long limit) {
        long safeLimit = Math.max(0, Math.min(limit, 5000));
        if (safeLimit == 0) {
            return List.of();
        }
        if (hotRankCache != null) {
            try {
                if (hotRankCache.size() > 0) {
                    return hotRankCache.getIds(0, safeLimit - 1);
                }
            } catch (Exception ignored) {
            }
        }
        return listHotIdsFromDb(safeLimit);
    }

    private List<Long> listHotIdsFromDb(long limit) {
        List<VideoStatsEntity> statsList = videoStatsMapper.selectList(new QueryWrapper<VideoStatsEntity>()
                .orderByDesc("hot_score")
                .orderByDesc("updated_at")
                .last("limit " + limit));
        if (statsList == null || statsList.isEmpty()) {
            return List.of();
        }

        List<Long> ids = statsList.stream()
                .map(VideoStatsEntity::getVideoId)
                .filter(Objects::nonNull)
                .toList();

        Set<Long> approved = videoMapper.selectBatchIds(ids).stream()
                .filter(v -> v != null && "APPROVED".equals(v.getAuditStatus()) && v.getId() != null)
                .map(VideoEntity::getId)
                .collect(Collectors.toSet());

        List<Long> ordered = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && approved.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    private Map<Long, VideoEntity> toVideoMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, VideoEntity> map = new HashMap<>();
        for (VideoEntity v : videoMapper.selectBatchIds(ids)) {
            if (v != null && v.getId() != null) {
                map.put(v.getId(), v);
            }
        }
        return map;
    }

    private Map<Long, VideoStatsEntity> toStatsMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, VideoStatsEntity> map = new HashMap<>();
        for (VideoStatsEntity s : videoStatsMapper.selectBatchIds(ids)) {
            if (s != null && s.getVideoId() != null) {
                map.put(s.getVideoId(), s);
            }
        }
        return map;
    }

    private RecommendationVideoDto toDto(VideoEntity video, VideoStatsEntity stats, boolean liked, boolean favorited) {
        VideoStatsEntity safeStats = stats == null ? new VideoStatsEntity() : stats;
        String url = minioStorageService.toPublicUrl(video.getVideoUrl());
        return new RecommendationVideoDto(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUploaderUserId() == null ? 0 : video.getUploaderUserId(),
                url,
                video.getAuditStatus(),
                video.getIsHot() != null && video.getIsHot() == 1,
                video.getCreatedAt() == null ? null : dtf.format(video.getCreatedAt()),
                tagsParser.parseTags(video.getTags()),
                safeLong(safeStats.getPlayCount()),
                safeLong(safeStats.getLikeCount()),
                safeLong(safeStats.getCommentCount()),
                safeLong(safeStats.getFavoriteCount()),
                liked,
                favorited,
                safeDouble(safeStats.getHotScore())
        );
    }

    private Set<Long> loadLikedVideoIds(long userId, List<Long> videoIds) {
        if (userId <= 0 || videoIds == null || videoIds.isEmpty()) {
            return Collections.emptySet();
        }
        return videoLikeMapper.selectList(new QueryWrapper<VideoLikeEntity>()
                        .eq("user_id", userId)
                        .in("video_id", videoIds))
                .stream()
                .map(VideoLikeEntity::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> loadFavoritedVideoIds(long userId, List<Long> videoIds) {
        if (userId <= 0 || videoIds == null || videoIds.isEmpty()) {
            return Collections.emptySet();
        }
        return videoFavoriteMapper.selectList(new QueryWrapper<VideoFavoriteEntity>()
                        .eq("user_id", userId)
                        .in("video_id", videoIds))
                .stream()
                .map(VideoFavoriteEntity::getVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static long parseCursor(String cursor) {
        if (cursor == null) {
            return 0L;
        }
        String s = cursor.trim();
        if (s.isEmpty()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(s));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static long safeLong(Long v) {
        return v == null ? 0L : Math.max(0L, v);
    }

    private static double safeDouble(Double v) {
        return v == null ? 0.0 : v;
    }
}
