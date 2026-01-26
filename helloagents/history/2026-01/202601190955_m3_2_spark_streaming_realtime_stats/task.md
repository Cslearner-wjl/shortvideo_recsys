# 任务清单: M3.2 Spark Streaming 实时统计并写 Redis

目录: `helloagents/plan/202601190955_m3_2_spark_streaming_realtime_stats/`

---

## 1. bigdata/streaming（Spark 作业）
- [√] 1.1 新增 `bigdata/streaming/pom.xml` 与工程骨架，支持打包可执行 Jar，验证 why.md#需求-实时聚合统计-场景-消费行为事件并增量更新统计
- [√] 1.2 实现 `BehaviorStreamingJob`：消费 Kafka `behavior-events`，按 `videoId` 聚合 delta（play/like/comment/favorite），验证 why.md#需求-实时聚合统计-场景-消费行为事件并增量更新统计
- [√] 1.3 实现 Redis Sink：写入 `stats:video:{id}` 与 `hot:videos`，支持 clamp（避免负数），验证 why.md#需求-实时聚合统计-场景-消费行为事件并增量更新统计
- [√] 1.4 编写 `bigdata/streaming/README.md`：运行参数、调试方法与常见问题

## 2. backend（/api/rank/hot 优先读 Redis）
- [√] 2.1 扩展 `HotRankProperties`：新增 `managed-by-streaming` 与 `stats-hash-prefix` 配置项
- [√] 2.2 调整 `HotRankScheduler`：当 `managed-by-streaming=true` 时不执行定时 refresh，避免覆盖 Streaming 写入的 ZSET
- [√] 2.3 调整 `HotRankService.page`：优先从 `stats:video:{id}` 读取计数与 hot_score，缺失则回退 DB，验证 why.md#需求-热门榜接口读取实时统计-场景-api-rank-hot-优先读取-redis
- [√] 2.4 更新 `backend/src/main/resources/application.yml`：补充配置项说明与默认值
  > 备注: 同步修复 `AuthController` profile，避免 `local` 下加载到缺失的 `AuthService` 导致测试启动失败。

## 3. 安全检查
- [√] 3.1 检查无明文密钥写入仓库；Kafka/Redis 连接仅通过环境变量/配置注入

## 4. 文档与知识库
- [√] 4.1 更新 `helloagents/wiki/modules/bigdata.md`：补充 M3.2 streaming 作业说明
- [√] 4.2 更新 `helloagents/wiki/modules/backend.md`：补充热门榜“Streaming 管理模式”说明与 Redis key 约定
- [√] 4.3 更新 `helloagents/wiki/data.md`：补充 Redis 统计 key 设计（只描述，不含敏感信息）
- [√] 4.4 更新 `helloagents/CHANGELOG.md`：记录 M3.2 变更

## 5. 测试与验收
- [√] 5.1 后端 `mvn -f backend/pom.xml test`（阻断性）
- [√] 5.2 Streaming `mvn -f bigdata/streaming/pom.xml -DskipTests package`（阻断性）
- [?] 5.3 端到端验收：触发点赞 -> Redis 变化 -> `/api/rank/hot` 返回变化（手工步骤记录在 streaming README）
  > 备注: 依赖本地 Docker/Redis/Kafka/Flume 与 Spark 环境，需按 `bigdata/streaming/README.md` 手工执行。
