# M3.2 Streaming（Spark -> Redis）

本目录提供 Spark Structured Streaming 作业：消费 Kafka `behavior-events`（每条消息为 1 行 JSON），按 `videoId` 聚合行为计数并写入 Redis：

- `stats:video:{videoId}`（Hash）
- `hot:videos`（ZSET）

> 说明：Streaming 模块本身不依赖 HDFS；HDFS 仅用于 Flume 落地（可选）。不做 ALS 训练。

---

## 1. 前置条件

1) 已启动依赖（至少 Redis + Kafka；如使用 Flume 采集则还需 Flume；如需 HDFS 落地则启动 HDFS）：

`docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d redis zookeeper kafka flume namenode datanode`

2) 后端行为日志写入路径与 Flume 挂载对齐（推荐）：

- Flume 采集：`deploy/data/behavior/behavior-events.log`
- 后端配置：设置环境变量 `BEHAVIOR_LOG_PATH=deploy/data/behavior/behavior-events.log`

3) （可选）HDFS 落地验证：
   - NameNode UI：`http://localhost:${HDFS_NAMENODE_UI_PORT}`
   - HDFS 路径：`/apps/behavior-events/`
   - 若 Flume HDFS Sink 无法启动，请先执行 `bigdata/streaming/bin/ensure_spark.sh`，确保 Spark 自带 Hadoop 客户端 jar 已缓存（docker-compose 会挂载该目录到 Flume classpath）。

3) 本机已安装 Spark（建议使用 Spark **3.5.x**，与本模块 `spark.version` 对齐；对 Java 17 兼容更好）。

> ⚠️ 注意：如果你的 Spark 版本较老（如 3.1.x）且运行在 Java 17，可能出现兼容性问题；建议升级 Spark 到 3.5.x（本仓库 `pom.xml` 默认以 3.5.1 为准）。

---

## 2. 构建

`mvn -f bigdata/streaming/pom.xml -DskipTests package`

产物：`bigdata/streaming/target/streaming-0.1.0.jar`（包含 redis/kafka connector，Spark 核心类由 Spark Distribution 提供）。

---

## 3. 运行

### 3.1 推荐：使用项目内固定 Spark 3.5.1（不影响系统 Spark）

本仓库提供脚本自动下载/缓存 Spark 3.5.1，并用该发行版的 `spark-submit` 运行作业（更适配 Java 17）：

```
# 方式A：从模块目录运行
bigdata/streaming/bin/run_streaming.sh

# 方式B：从 scripts/ 统一入口运行
scripts/run_m3_2_streaming.sh
```

离线/自定义发行版场景（跳过下载）：

```
export SPARK_DIST=/path/to/spark-3.5.1-bin-hadoop3
scripts/run_m3_2_streaming.sh
```

> 缓存目录默认在 `bigdata/streaming/.spark/`（已被 `.gitignore` 忽略）。

### 3.2 环境变量

- `KAFKA_BOOTSTRAP`：默认 `localhost:9093`（对应 docker compose 对外端口）
- `KAFKA_TOPIC`：默认 `behavior-events`
- `REDIS_HOST`：默认 `127.0.0.1`
- `REDIS_PORT`：默认 `6379`
- `REDIS_PASSWORD`：可选
- `REDIS_DB`：默认 `0`
- `CHECKPOINT_DIR`：默认 `/tmp/shortvideo_recsys/checkpoints/m3_2`
- `SPARK_TRIGGER_INTERVAL`：默认 `5 seconds`
- `STATS_HASH_PREFIX`：默认 `stats:video:`
- `HOT_ZSET_KEY`：默认 `hot:videos`
- `HOT_W_PLAY`/`HOT_W_LIKE`/`HOT_W_COMMENT`/`HOT_W_FAVORITE`：hot_score 权重（默认 1/3/5/4）

### 3.3 手动 spark-submit（本地模式，可选）

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

---

## 4. 调试要点

1) 观察 Spark 日志中是否成功连接 Kafka/Redis。
2) 观察 Redis：

```
redis-cli -h 127.0.0.1 -p 6379 HGETALL stats:video:1
redis-cli -h 127.0.0.1 -p 6379 ZREVRANGE hot:videos 0 9 WITHSCORES
```

3) Kafka 消息验证（可选）：

```
docker exec -it shortvideo-recsys-kafka bash -lc "kafka-console-consumer --bootstrap-server kafka:9092 --topic behavior-events --from-beginning --max-messages 1"
```

---

## 5. 端到端验收流程（示例：触发点赞）

1) 启动后端（`docker` profile）与依赖，并确保行为日志写入到 `deploy/data/behavior/behavior-events.log`，Flume 正在采集并写入 Kafka。
2) 启动本 Streaming 作业。
3) 用用户登录获取 JWT，并确保存在 1 个 `APPROVED` 的视频 `videoId`。
4) 触发点赞：
   - `POST /api/videos/{videoId}/like`（携带 `Authorization: Bearer <token>`）
5) 验证 Redis：
   - `HGET stats:video:{videoId} like_count` 增加 1
   - `ZSCORE hot:videos {videoId}` 增加（默认权重 +3）
6) 验证接口：
   - `GET /api/rank/hot` 返回中的 `likeCount/hotScore` 与 Redis 对齐（后端优先读取 Redis）
