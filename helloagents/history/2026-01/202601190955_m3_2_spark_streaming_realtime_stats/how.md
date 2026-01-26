# 技术设计: M3.2 Spark Streaming 实时统计并写 Redis

## 技术方案

### 核心技术

- Spark Structured Streaming（优先；保留未来切换 DStream 的可能）
- Kafka Source：`spark-sql-kafka-0-10`
- Redis Sink：Jedis（pipeline + Lua clamp）

### Spark 作业目录结构（计划）

```
bigdata/streaming/
├── pom.xml
├── README.md
└── src/main/java/com/shortvideo/recsys/streaming/
    ├── BehaviorStreamingJob.java
    ├── RedisSink.java
    ├── RedisSinkConfig.java
    └── BehaviorEventSchemas.java
```

### Kafka 消费方式

- **Topic:** `behavior-events`
- **消息体:** 每条消息 value 为 1 行 JSON（来源：后端行为日志 JSON Lines，经 Flume TAILDIR 写入 Kafka）
- **Structured Streaming Source:**
  - `format("kafka")`
  - `option("kafka.bootstrap.servers", ${KAFKA_BOOTSTRAP})`
  - `option("subscribe", "behavior-events")`
  - `option("startingOffsets", "latest")`（验收链路默认；如需全量回放可改为 `earliest`）
- **Checkpoint:** 本地文件系统目录（不引入 HDFS），例如 `/tmp/shortvideo_recsys/checkpoints/m3_2`

### 窗口聚合策略

采用 **micro-batch 增量窗口**（以 Trigger 间隔作为近实时窗口），在 `foreachBatch` 中按 `videoId` 聚合本批次 delta：

- 触发间隔：默认 `5 seconds`（可配置）
- 聚合维度：`videoId`
- 聚合指标：
  - `PLAY` → `play_count += 1`
  - `LIKE` / `UNLIKE` → `like_count += 1 / -1`
  - `FAVORITE` / `UNFAVORITE` → `favorite_count += 1 / -1`
  - `COMMENT` → `comment_count += 1`
- 事件时间字段：优先使用 `actionTs`（毫秒时间戳）用于未来 watermark/late data 扩展；本阶段验收以 micro-batch 增量为主（不做 late data 纠偏）。

### Redis Key 设计

1) 单视频统计 Hash：

- **Key:** `stats:video:{videoId}`
- **Fields（字符串）:**
  - `play_count`（long）
  - `like_count`（long）
  - `comment_count`（long）
  - `favorite_count`（long）
  - `hot_score`（double）
  - `updated_at_ms`（long，便于调试）

2) 热门榜 ZSET：

- **Key:** `hot:videos`
- **member:** `{videoId}`
- **score:** `hot_score`（double）

3) 计数不为负（clamp）

对 `UNLIKE/UNFAVORITE` 的负向增量使用 Lua 脚本对 `HINCRBY` 结果进行 clamp，避免负数。

### hot_score 计算（简化）

按增量贡献更新（非全量重算），权重默认与后端一致：

```
delta_hot = play*1.0 + like*3.0 + comment*5.0 + favorite*4.0
```

写入方式：
- `HINCRBYFLOAT stats:video:{id} hot_score delta_hot`
- `ZINCRBY hot:videos delta_hot {id}`

## 架构决策 ADR

### ADR-001: 采用 Structured Streaming + foreachBatch 写 Redis
**上下文:** 需要在不引入 HDFS 的前提下实现准实时统计写 Redis，并保持实现简单可验收。
**决策:** 采用 Structured Streaming 读取 Kafka，使用 `foreachBatch` 做批次内聚合并写 Redis。
**理由:** micro-batch 天然适配“增量统计”；实现简单；checkpoint 可用本地目录；便于本地调试。
**替代方案:** DStream（Spark Streaming 旧 API） → 拒绝原因: Java 实现复杂度更高，且未来与 Structured 生态割裂。
**影响:** 语义为 at-least-once；若需严格去重需后续引入 eventId 去重策略（Redis TTL Set/Bitmap 等）。

## 后端改造设计

### 配置项

- `app.hot-rank.managed-by-streaming`（默认 false）
  - true：热门榜由 Streaming 写入 `hot:videos` 维护；后端不执行定时 refresh；接口读取时优先从 Redis Hash 读取统计
  - false：保持原 M1.5 逻辑（可定时从 DB 计算并刷新 Redis ZSET），作为兜底
- `app.hot-rank.stats-hash-prefix`（默认 `stats:video:`）

### /api/rank/hot 读取策略

- 仍通过 `hot:videos` ZSET 取排序后的 `video_id`（已有 `RedisHotRankCache`）
- 当 `managed-by-streaming=true` 且 Redis 可用时：
  - 优先从 `stats:video:{id}` 读取计数与 hot_score
  - Redis 缺失则回退到 DB 的 `video_stats`（便于混合阶段上线）

## 安全与性能

- Redis 写入使用 pipeline 降低 RTT
- 不引入任何明文密钥：Kafka/Redis 连接信息通过环境变量/配置注入

## 运行命令（本地）

### 1) 启动依赖（MySQL/Redis/Kafka/Flume）

（示例）在仓库根目录：

1. `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d mysql redis zookeeper kafka flume`
2. 后端设置（重要）：将 `BEHAVIOR_LOG_PATH` 指向 `deploy/data/behavior/behavior-events.log`（与 Flume 挂载路径对齐）

### 2) 构建 Streaming 作业 Jar

`mvn -f bigdata/streaming/pom.xml -DskipTests package`

### 3) 运行 Spark 作业（本机 Spark）

```
export SPARK_SUBMIT_OPTS='--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED'
export KAFKA_BOOTSTRAP=localhost:9093
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export CHECKPOINT_DIR=/tmp/shortvideo_recsys/checkpoints/m3_2
export SPARK_TRIGGER_INTERVAL=5 seconds

spark-submit \
  --class com.shortvideo.recsys.streaming.BehaviorStreamingJob \
  --master local[*] \
  bigdata/streaming/target/streaming-0.1.0.jar
```

## 验收步骤（端到端）

1. 启动依赖 + 后端（`docker` profile），并确认 Flume 正在采集 `deploy/data/behavior/behavior-events.log` 写入 Kafka `behavior-events`
2. 启动 Streaming 作业
3. 准备 1 个已审核通过的视频（`videoId`），并使用用户登录获取 JWT
4. 触发 1 次点赞：
   - `POST /api/videos/{videoId}/like`（携带 JWT）
5. 观察 Redis 变化：
   - `HGET stats:video:{videoId} like_count` 应增加 1
   - `ZSCORE hot:videos {videoId}` 应增加（按权重）
6. 观察接口变化：
   - `GET /api/rank/hot?page=1&pageSize=20` 中该视频 `likeCount/hotScore` 应反映 Redis 的增量（优先读取）
