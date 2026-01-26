// M4：将 ALS 结果写入 Redis（rec:user:{userId}）。
package com.shortvideo.recsys.batch;

import java.util.List;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Pipeline;

public class RedisRecommendationWriter {
    public static class Config implements java.io.Serializable {
        private final String host;
        private final int port;
        private final String password;
        private final int db;
        private final String keyPrefix;
        private final int ttlSeconds;

        public Config(String host, int port, String password, int db, String keyPrefix, int ttlSeconds) {
            this.host = host;
            this.port = port;
            this.password = password;
            this.db = db;
            this.keyPrefix = keyPrefix;
            this.ttlSeconds = ttlSeconds;
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

        public String keyPrefix() {
            return keyPrefix;
        }

        public int ttlSeconds() {
            return ttlSeconds;
        }

        public static Config fromEnv() {
            String host = env("REDIS_HOST", "127.0.0.1");
            int port = envInt("REDIS_PORT", 6379);
            String password = env("REDIS_PASSWORD", "");
            int db = envInt("REDIS_DB", 0);
            String keyPrefix = env("REDIS_KEY_PREFIX", "rec:user:");
            int ttlSeconds = envInt("REDIS_TTL_SECONDS", 86400);
            return new Config(host, port, password, db, keyPrefix, ttlSeconds);
        }

        private static String env(String key, String defaultValue) {
            String v = System.getenv(key);
            if (v == null) {
                return defaultValue;
            }
            String s = v.trim();
            return s.isEmpty() ? defaultValue : s;
        }

        private static int envInt(String key, int defaultValue) {
            String v = System.getenv(key);
            if (v == null) {
                return defaultValue;
            }
            String s = v.trim();
            if (s.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(s);
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
    }

    public static void writeOne(Jedis jedis, Config cfg, long userId, List<Long> videoIds) {
        if (jedis == null || cfg == null) {
            return;
        }
        String key = cfg.keyPrefix() + userId;
        Pipeline p = jedis.pipelined();
        p.del(key);
        if (videoIds != null) {
            for (Long vid : videoIds) {
                if (vid != null && vid > 0) {
                    p.rpush(key, String.valueOf(vid));
                }
            }
        }
        if (cfg.ttlSeconds() > 0) {
            p.expire(key, cfg.ttlSeconds());
        }
        p.sync();
    }

    public static Jedis connect(Config cfg) {
        HostAndPort hp = new HostAndPort(cfg.host(), cfg.port());
        JedisClientConfig clientCfg = DefaultJedisClientConfig.builder()
                .password((cfg.password() == null || cfg.password().trim().isEmpty()) ? null : cfg.password())
                .database(cfg.db())
                .build();
        return new Jedis(hp, clientCfg);
    }

    public static void writePartition(java.util.Iterator<org.apache.spark.sql.Row> rows, Config cfg) {
        try (Jedis jedis = connect(cfg)) {
            Pipeline p = jedis.pipelined();
            int n = 0;
            while (rows.hasNext()) {
                org.apache.spark.sql.Row r = rows.next();
                if (r == null) {
                    continue;
                }
                Long userId = toLong(r.getAs("user_id"));
                List<?> ids = r.getList(r.fieldIndex("video_ids"));
                if (userId == null || userId <= 0 || ids == null || ids.isEmpty()) {
                    continue;
                }

                String key = cfg.keyPrefix() + userId;
                p.del(key);
                for (Object o : ids) {
                    Long vid = toLong(o);
                    if (vid != null && vid > 0) {
                        p.rpush(key, String.valueOf(vid));
                    }
                }
                if (cfg.ttlSeconds() > 0) {
                    p.expire(key, cfg.ttlSeconds());
                }

                n++;
                if (n % 200 == 0) {
                    p.sync();
                }
            }
            p.sync();
        }
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof Integer) {
            return ((Integer) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return null;
    }
}
