# 技术设计: M4 Spark ALS 离线协同过滤推荐 → Redis

## 技术方案

### 核心技术
- Spark（本地模式 / standalone）：MLlib ALS（`implicitPrefs=true`）
- 输入数据来源：MySQL（JDBC 读取 `user_actions`）或本地 CSV（最小可复现）
- 输出存储：Redis（推荐结果 key：`rec:user:{userId}`）
- 后端：Spring Boot（`docker` profile 启用 MySQL/Redis）

### Spark 作业目录结构（规划）
在 `bigdata/batch/` 新增一个 Maven 模块，风格对齐 `bigdata/streaming/`：
```text
bigdata/batch/
  pom.xml
  README.md
  bin/
    ensure_spark.sh
    run_als.sh
  src/main/java/com/shortvideo/recsys/batch/
    AlsTrainJob.java
    JdbcUserActionsReader.java
    RedisRecommendationWriter.java
  src/main/resources/
    log4j2.xml
  sample-data/
    user_actions.csv
```

---

## 训练数据构建（从 user_actions 导出）

### 输入表（现状）
`backend` 已存在 `user_actions` 表（MySQL / H2 schema）。

### 抽取字段与时间窗口（建议默认）
- 字段：`user_id`, `video_id`, `action_type`, `action_time`
- 时间窗口：默认最近 `N` 天（建议 `7`），可配置（例如 `ALS_TRAIN_DAYS=7`）
- 行过滤：
  - `user_id` / `video_id` 非空
  - action_type ∈ {PLAY, LIKE, COMMENT, FAVORITE}

### 评分映射规则（implicit feedback）
将离散行为映射为正反馈强度，形成 `rating`：
- PLAY → 1
- LIKE → 2
- COMMENT → 3
- FAVORITE → 4

构建训练评分的两种可选策略（推荐方案 1）：
1. **方案 1（推荐）: 按 (user_id, video_id) 聚合求和**  
   `rating = Σ weight(action_type)`，适合行为重复发生的累积偏好。
2. 方案 2: 取最大权重  
   `rating = max(weight)`，更强调“最强反馈”。

---

## ALS 参数（可配置，默认值建议）
使用 MLlib ALS 隐式反馈模式：
- `implicitPrefs = true`
- `rank = 20`
- `maxIter = 10`
- `regParam = 0.1`
- `alpha = 40.0`
- `coldStartStrategy = drop`

约束满足：
- 不做模型自动调参（参数通过 env/args 配置）
- 不做复杂评估指标（可选输出简单 hit@K）

---

## 输出格式与写 Redis 的方式

### 推荐结果生成
对每个用户生成 TopN（建议默认 `50`，可配置）：
- 输出结构：`(user_id, [video_id1, video_id2, ...])`
- 生成方式：`recommendForAllUsers(topN)` 或按用户分批生成

### Redis key 设计
- 推荐列表：`rec:user:{userId}`（Redis List 或 String(JSON)）
  - 推荐：使用 **List**（便于后端分页/读取）
  - 写入：`DEL key` → `RPUSH key v1 v2 ... vN` → `EXPIRE key ttlSeconds`
- TTL：建议 `6h` 或 `24h`（可配置）

（可选）写入调试信息：
- `rec:user:{userId}:meta`（Hash / String），记录 `model_ts`、`topN`、`train_window_days` 等

### 写入一致性与性能
- 使用 pipeline/批量写入（Jedis/Lettuce 客户端任选，batch 作业侧独立引入）
- 控制写入规模：可限制仅对活跃用户写入（例如窗口内有行为的用户）

---

## 后端开关配置（规划）
目标：**开关开启时优先 ALS，否则规则推荐**。

建议新增/扩展配置：
- `app.recommendations.als.enabled`（默认 false，`docker` 可开）
- `app.recommendations.als.redis-prefix`（默认 `rec:user:`）
- `app.recommendations.als.topn`（默认 50）
- `app.recommendations.als.ttl-seconds`（仅用于批作业写入或后端兜底写入）

读取逻辑（建议）：
1. 若 `als.enabled=false` → 直接规则推荐
2. `als.enabled=true` → 读取 `rec:user:{userId}`：
   - 命中且非空 → 按顺序取视频详情/统计并返回
   - 未命中/空 → 规则推荐（保持现有策略）

---

## 运行命令（规划）
以复用 `bigdata/streaming/bin/ensure_spark.sh` 的方式提供：
- 构建：`mvn -pl bigdata/batch -am package -DskipTests`
- 运行（本地 CSV）：`bigdata/batch/bin/run_als.sh --mode local --input sample-data/user_actions.csv --redis-host 127.0.0.1 --redis-port 6379`
- 运行（JDBC）：`bigdata/batch/bin/run_als.sh --mode jdbc --jdbc-url ... --jdbc-user ... --jdbc-password ...`

---

## 验收步骤（端到端）
提供一条可复现验收链路（目标是“触发行为 → DB 产生 user_actions → 离线训练 → Redis 变化 → 接口响应变化”）：
1. 启动依赖（MySQL/Redis）与后端（`docker` profile），确保写入真实 `user_actions`。
2. 通过接口触发点赞/收藏/评论/播放，产生 `user_actions` 记录。
3. 运行 ALS batch 作业（JDBC 读取 MySQL），写入 Redis `rec:user:{userId}`。
4. 观察 Redis key 变化（推荐列表写入成功，长度=TopN）。
5. 开启后端 ALS 读取开关，请求 `GET /api/recommendations`，验证返回顺序与 Redis 推荐一致；关闭开关验证回退规则推荐。

