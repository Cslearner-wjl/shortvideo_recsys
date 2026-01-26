# 变更提案: M3.2 Spark Streaming 实时统计并写 Redis

## 需求背景

当前系统已具备行为事件日志（JSON Lines）与 Flume -> Kafka `behavior-events` 采集链路，但“实时统计/热门榜”仍主要依赖后端定时从数据库聚合并刷新 Redis ZSET 的方式，无法做到准实时更新，也无法支撑更细粒度的实时指标演进。

本变更在不引入 HDFS、且不做 ALS 训练的前提下，增加 Spark Streaming 作业消费 Kafka `behavior-events`，将按 `video_id` 聚合的行为计数写入 Redis，并同步维护热门榜 ZSET，使 `GET /api/rank/hot` 优先读取 Redis 的实时结果。

## 变更内容

1. 新增 `bigdata/streaming`：Spark Structured Streaming 作业，消费 Kafka `behavior-events`
2. 实时聚合并写 Redis：
   - Hash：`stats:video:{videoId}`（play/like/comment/favorite/hot_score）
   - ZSET：`hot:videos`（score=hot_score）
3. 后端热门榜接口优先从 Redis 读取统计（可配置为“由 Streaming 管理”模式），避免与定时刷新逻辑冲突

## 影响范围

- **模块:**
  - `bigdata`（新增 streaming 子目录）
  - `backend`（热门榜读取逻辑与配置）
- **文件:**
  - `bigdata/streaming/**`
  - `backend/src/main/java/com/shortvideo/recsys/backend/rank/**`
  - `backend/src/main/resources/application.yml`
  - `helloagents/wiki/*`（同步知识库）
- **API:**
  - 不新增接口；`GET /api/rank/hot` 行为调整为“优先从 Redis 实时统计读取”
- **数据:**
  - 不引入新表；新增 Redis Key 约定

## 核心场景

### 需求: 实时聚合统计
**模块:** bigdata

#### 场景: 消费行为事件并增量更新统计
Kafka `behavior-events` 每条消息为 1 个行为事件（PLAY/LIKE/UNLIKE/FAVORITE/UNFAVORITE/COMMENT）。Spark 作业按 micro-batch 聚合为增量（delta），对 `stats:video:{id}` 执行字段级增量更新，并同步更新 `hot:videos` 的 score。

### 需求: 热门榜接口读取实时统计
**模块:** backend

#### 场景: /api/rank/hot 优先读取 Redis
后端按热门 ZSET（`hot:videos`）取 TopN `video_id`，并优先从 `stats:video:{id}` 读取计数与 `hot_score`；在未启用 Streaming 管理模式时，保留原数据库聚合刷新兜底能力。

## 风险评估

- **风险:** Streaming 作业重启可能导致重复消费（at-least-once）引起计数偏大
- **缓解:** 依赖 Spark checkpoint；本阶段不引入全量去重（可在后续用 `eventId` 做 Redis 去重集合/TLL 进一步增强）
- **风险:** 与后端定时刷新热门榜逻辑冲突（刷新会覆盖 Streaming 写入的 ZSET）
- **缓解:** 增加 `app.hot-rank.managed-by-streaming` 配置，启用后禁用定时刷新与手动 refresh 行为（no-op），并在接口读取侧优先走 Redis 统计

