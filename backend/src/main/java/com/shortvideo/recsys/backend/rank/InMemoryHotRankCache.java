package com.shortvideo.recsys.backend.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"docker", "test"})
@ConditionalOnProperty(prefix = "app.hot-rank", name = "cache", havingValue = "memory")
public class InMemoryHotRankCache implements HotRankCache {
    private volatile List<Long> orderedIds = List.of();

    @Override
    public void replaceTop(List<HotRankEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            this.orderedIds = List.of();
            return;
        }
        List<Long> ids = entries.stream().map(e -> e.videoId()).map(Long::valueOf).toList();
        this.orderedIds = Collections.unmodifiableList(new ArrayList<>(ids));
    }

    @Override
    public List<Long> getIds(long startInclusive, long endInclusive) {
        List<Long> current = orderedIds;
        if (current.isEmpty()) {
            return List.of();
        }

        long safeStart = Math.max(0L, startInclusive);
        long safeEnd = Math.max(safeStart, endInclusive);
        if (safeStart >= current.size()) {
            return List.of();
        }

        int from = (int) safeStart;
        int toExclusive = (int) Math.min((long) current.size(), safeEnd + 1);
        return current.subList(from, toExclusive);
    }

    @Override
    public long size() {
        return orderedIds.size();
    }
}
