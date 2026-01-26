# bigdata

## 目的

构建行为日志链路与后续分析能力，支撑实时统计与推荐特征提取。

## 模块概览

- **职责:** 行为日志采集与规范化、Flume -> Kafka 采集链路、Kafka -> Flume -> HDFS 落地、Streaming/离线任务预留
- **职责补充:** M3.2 新增 Spark Streaming 实时统计作业（Kafka -> Redis）
- **职责补充:** M4 新增 Spark MLlib ALS 离线推荐作业（MySQL `user_actions`/CSV -> Redis）
- **状态:** 开发中（已落地行为日志链路 + M3.2 实时统计 + M4 离线 ALS 推荐作业）
- **最后更新:** 2026-01-22

---

## 规范（部分落地）

### 需求：事件格式统一

统一使用 JSON Lines schema，行为事件字段与 `behavior-events` topic 对齐（见 `helloagents/wiki/data.md`）。

### 需求：行为日志落盘（已落地）

后端追加写入本地日志文件（默认 `./logs/behavior-events.log`），可通过 `app.behavior-log.path` 或 `BEHAVIOR_LOG_PATH` 指定到 `deploy/data/behavior/behavior-events.log` 以便 Flume 采集。

### 需求：实时统计（M3.2，已落地）

消费 Kafka `behavior-events`，对 `videoId` 做增量聚合并写入 Redis：

- Hash：`stats:video:{videoId}`（play/like/comment/favorite/hot_score）
- ZSET：`hot:videos`（热门榜分数）

Spark 作业目录：`bigdata/streaming/`（Structured Streaming + foreachBatch）。

#### 推荐运行方式（项目内固定 Spark 3.5.1）

为避免本机/系统 Spark 版本差异导致的兼容性问题（尤其是 Spark 3.1.x + Java 17），推荐使用仓库脚本在项目内固定 Spark 3.5.1 并运行：

- `scripts/run_m3_2_streaming.sh`（内部使用 `bigdata/streaming/bin/run_streaming.sh`）
- 缓存目录默认：`bigdata/streaming/.spark/`（已忽略，不入库）

离线或自定义 Spark 发行版时，可设置 `SPARK_DIST=/path/to/spark-3.5.1-bin-hadoop3` 跳过下载。

### 需求：HDFS 落地（已落地）

Kafka `behavior-events` 通过 Flume Kafka Source 落地到 HDFS（本地 NameNode/DataNode），作为行为日志归档。
本地环境需确保 Spark 发行版已缓存（用于提供 Hadoop 客户端 jar）。

### 需求：实时计算（规划）

Kafka -> Streaming -> Redis/MySQL：实现实时热门榜/实时计数与推荐缓存。

### 需求：离线 ALS（规划/已可运行）

从 `user_actions`（JDBC）或本地 CSV 构建隐式反馈评分，训练 Spark MLlib ALS（implicit feedback），输出用户 TopN 写入 Redis：

- `rec:user:{userId}`（List）

作业目录：`bigdata/batch/`。

---

## 依赖（部分落地）

- Kafka + Zookeeper
- Flume
- HDFS（本地）
- Spark / Spark Streaming / MLlib ALS
- Redis

## 测试

- 行为链路验收: `scripts/acceptance_m3_2_streaming.sh`
- Kafka 消费验证: `scripts/verify_behavior_events.sh`
- 报告输出: `docs/tests/bigdata/`
