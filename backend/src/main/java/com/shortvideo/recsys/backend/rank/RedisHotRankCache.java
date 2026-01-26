package com.shortvideo.recsys.backend.rank;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

@Component
@Profile({"docker", "test"})
@ConditionalOnProperty(prefix = "app.hot-rank", name = "cache", havingValue = "redis", matchIfMissing = true)
public class RedisHotRankCache implements HotRankCache {
    private final StringRedisTemplate redisTemplate;
    private final HotRankProperties properties;

    public RedisHotRankCache(StringRedisTemplate redisTemplate, HotRankProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void replaceTop(List<HotRankEntry> entries) {
        String key = properties.getRedisKey();
        redisTemplate.delete(key);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        for (HotRankEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            zset.add(key, String.valueOf(entry.videoId()), entry.score());
        }
    }

    @Override
    public List<Long> getIds(long startInclusive, long endInclusive) {
        String key = properties.getRedisKey();
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        var range = zset.reverseRange(key, startInclusive, endInclusive);
        if (range == null || range.isEmpty()) {
            return List.of();
        }
        return range.stream().filter(Objects::nonNull).map(Long::parseLong).toList();
    }

    @Override
    public long size() {
        Long size = redisTemplate.opsForZSet().zCard(properties.getRedisKey());
        return size == null ? 0L : size;
    }
}
