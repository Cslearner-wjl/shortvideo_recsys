// Redis 写入配置（通过环境变量注入，避免硬编码敏感信息）。
package com.shortvideo.recsys.streaming;

import java.io.Serializable;

public class RedisSinkConfig implements Serializable {
    private final String host;
    private final int port;
    private final String password;
    private final int db;
    private final String statsHashPrefix;
    private final String hotZsetKey;
    private final double wPlay;
    private final double wLike;
    private final double wComment;
    private final double wFavorite;

    public RedisSinkConfig(
            String host,
            int port,
            String password,
            int db,
            String statsHashPrefix,
            String hotZsetKey,
            double wPlay,
            double wLike,
            double wComment,
            double wFavorite
    ) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.db = db;
        this.statsHashPrefix = statsHashPrefix;
        this.hotZsetKey = hotZsetKey;
        this.wPlay = wPlay;
        this.wLike = wLike;
        this.wComment = wComment;
        this.wFavorite = wFavorite;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String password() {
        return password;
    }

    public int db() {
        return db;
    }

    public String statsHashPrefix() {
        return statsHashPrefix;
    }

    public String hotZsetKey() {
        return hotZsetKey;
    }

    public double wPlay() {
        return wPlay;
    }

    public double wLike() {
        return wLike;
    }

    public double wComment() {
        return wComment;
    }

    public double wFavorite() {
        return wFavorite;
    }

    public static RedisSinkConfig fromEnv() {
        String host = env("REDIS_HOST", "127.0.0.1");
        int port = envInt("REDIS_PORT", 6379);
        String password = env("REDIS_PASSWORD", "");
        int db = envInt("REDIS_DB", 0);
        String statsHashPrefix = env("STATS_HASH_PREFIX", "stats:video:");
        String hotZsetKey = env("HOT_ZSET_KEY", "hot:videos");

        double wPlay = envDouble("HOT_W_PLAY", 1.0);
        double wLike = envDouble("HOT_W_LIKE", 3.0);
        double wComment = envDouble("HOT_W_COMMENT", 5.0);
        double wFavorite = envDouble("HOT_W_FAVORITE", 4.0);

        String safePassword = password == null || password.isBlank() ? null : password;
        return new RedisSinkConfig(host, port, safePassword, db, statsHashPrefix, hotZsetKey, wPlay, wLike, wComment, wFavorite);
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? defaultValue : v.trim();
    }

    private static int envInt(String key, int defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static double envDouble(String key, double defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}

