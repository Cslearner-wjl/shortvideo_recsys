package com.shortvideo.recsys.backend.rank;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"docker", "test"})
public class HotRankScheduler {
    private final HotRankProperties properties;
    private final HotRankService hotRankService;

    public HotRankScheduler(HotRankProperties properties, HotRankService hotRankService) {
        this.properties = properties;
        this.hotRankService = hotRankService;
    }

    @Scheduled(fixedDelayString = "${app.hot-rank.refresh-interval-ms:300000}")
    public void refreshHotRank() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.isManagedByStreaming()) {
            return;
        }
        hotRankService.refresh();
    }
}
