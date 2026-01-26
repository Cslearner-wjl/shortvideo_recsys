// Redis Sink：将每个 micro-batch 的 video 维度增量写入 Hash 与热门 ZSET。
package com.shortvideo.recsys.streaming;

import java.util.Iterator;
import org.apache.spark.sql.Row;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;

public final class RedisSink {
    private RedisSink() {
    }

    private static final String FIELD_PLAY = "play_count";
    private static final String FIELD_LIKE = "like_count";
    private static final String FIELD_COMMENT = "comment_count";
    private static final String FIELD_FAVORITE = "favorite_count";
    private static final String FIELD_HOT = "hot_score";
    private static final String FIELD_UPDATED_AT = "updated_at_ms";

    private static final String LUA_HINCRBY_CLAMP_NONNEG = ""
            + "local key = KEYS[1]\n"
            + "local field = ARGV[1]\n"
            + "local delta = tonumber(ARGV[2])\n"
            + "local v = redis.call('HINCRBY', key, field, delta)\n"
            + "if v < 0 then\n"
            + "  redis.call('HSET', key, field, 0)\n"
            + "  return 0\n"
            + "end\n"
            + "return v\n";

    public static void writePartition(Iterator<Row> rows, RedisSinkConfig config) {
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password(config.password())
                .database(config.db())
                .build();
        JedisPooled jedis = new JedisPooled(new HostAndPort(config.host(), config.port()), clientConfig);

        Pipeline p = jedis.pipelined();
        long nowMs = System.currentTimeMillis();

        while (rows.hasNext()) {
            Row r = rows.next();
            long videoId = r.getAs("videoId");
            long playDelta = r.getAs("playDelta");
            long likeDelta = r.getAs("likeDelta");
            long commentDelta = r.getAs("commentDelta");
            long favoriteDelta = r.getAs("favoriteDelta");
            double hotDelta = r.getAs("hotDelta");

            String statsKey = config.statsHashPrefix() + videoId;
            String member = String.valueOf(videoId);

            if (playDelta != 0) {
                p.hincrBy(statsKey, FIELD_PLAY, playDelta);
            }
            if (commentDelta != 0) {
                p.hincrBy(statsKey, FIELD_COMMENT, commentDelta);
            }

            if (likeDelta != 0) {
                p.eval(LUA_HINCRBY_CLAMP_NONNEG, 1, statsKey, FIELD_LIKE, String.valueOf(likeDelta));
            }
            if (favoriteDelta != 0) {
                p.eval(LUA_HINCRBY_CLAMP_NONNEG, 1, statsKey, FIELD_FAVORITE, String.valueOf(favoriteDelta));
            }

            if (hotDelta != 0.0) {
                p.hincrByFloat(statsKey, FIELD_HOT, hotDelta);
                p.zincrby(config.hotZsetKey(), hotDelta, member);
            }

            p.hset(statsKey, FIELD_UPDATED_AT, String.valueOf(nowMs));
        }

        p.sync();
        jedis.close();
    }
}

