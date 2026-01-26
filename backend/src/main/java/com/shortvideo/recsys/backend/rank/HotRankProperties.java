package com.shortvideo.recsys.backend.rank;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.hot-rank")
public class HotRankProperties {
    private boolean enabled = false;
    private long refreshIntervalMs = 5 * 60 * 1000L;
    private int topn = 1000;
    private String redisKey = "hot:videos";
    private String cache = "redis";
    private boolean managedByStreaming = false;
    private String statsHashPrefix = "stats:video:";
    private final Weights weights = new Weights();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public void setRefreshIntervalMs(long refreshIntervalMs) {
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public int getTopn() {
        return topn;
    }

    public void setTopn(int topn) {
        this.topn = topn;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public void setRedisKey(String redisKey) {
        this.redisKey = redisKey;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public boolean isManagedByStreaming() {
        return managedByStreaming;
    }

    public void setManagedByStreaming(boolean managedByStreaming) {
        this.managedByStreaming = managedByStreaming;
    }

    public String getStatsHashPrefix() {
        return statsHashPrefix;
    }

    public void setStatsHashPrefix(String statsHashPrefix) {
        this.statsHashPrefix = statsHashPrefix;
    }

    public Weights getWeights() {
        return weights;
    }

    public static class Weights {
        private double play = 1.0;
        private double like = 3.0;
        private double comment = 5.0;
        private double favorite = 4.0;

        public double getPlay() {
            return play;
        }

        public void setPlay(double play) {
            this.play = play;
        }

        public double getLike() {
            return like;
        }

        public void setLike(double like) {
            this.like = like;
        }

        public double getComment() {
            return comment;
        }

        public void setComment(double comment) {
            this.comment = comment;
        }

        public double getFavorite() {
            return favorite;
        }

        public void setFavorite(double favorite) {
            this.favorite = favorite;
        }
    }
}
