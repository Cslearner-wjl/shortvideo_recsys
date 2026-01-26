# 变更提案: M4 Spark ALS 离线协同过滤推荐 → Redis

## 需求背景
当前推荐主要依赖规则策略（标签偏好 + 热门榜 + 随机兜底），个性化能力有限。希望增加一套**离线协同过滤（Spark MLlib ALS，implicit feedback）**训练流水线，周期性产出每个用户的 TopN 视频推荐结果写入 Redis，并在后端推荐接口中按开关优先读取 ALS 结果，未命中或关闭则回退到规则推荐。

## 变更内容
1. 新增 `bigdata/batch`：Spark MLlib ALS 离线训练作业（implicit feedback）。
2. 训练输入从 `user_actions` 构建：`user_id, video_id, rating`（由 play/like/comment/favorite 映射）。
3. 训练输出：每个用户 TopN `video_id` 列表写入 Redis `rec:user:{userId}`。
4. 后端推荐接口读取策略调整：开关开启时优先读取 ALS（Redis），否则走规则推荐。
5. 提供最小可复现实验数据与本地运行说明，并给出端到端验收流程。

## 影响范围
- **模块:** `bigdata`（新增 batch 作业）、`backend`（推荐读取逻辑与配置）
- **文件（预估）:**
  - `bigdata/batch/**`（新增）
  - `backend/src/main/java/com/shortvideo/recsys/backend/recommendation/**`（调整/新增）
  - `backend/src/main/resources/application.yml`（新增/扩展开关与参数）
  - `deploy/docker-compose.yml`（如需补充批处理运行说明，不强制修改）
- **API:**
  - `GET /api/recommendations`：读取策略变更（优先 ALS，fallback 规则）
- **数据:**
  - 读取 `user_actions`（离线训练输入）
  - 写入 Redis（推荐结果缓存）

## 核心场景

### 需求: ALS 离线训练与写入 Redis
**模块:** bigdata/batch
周期性从 `user_actions` 构建隐式反馈训练数据，训练 ALS 模型，生成每个用户 TopN 推荐列表写入 Redis。

#### 场景: 训练作业本地运行（最小可复现）
给定一份最小样例数据（用户行为→评分），可在本地运行 Spark 作业并看到 Redis 中写入 `rec:user:{userId}`。
- 预期结果1：作业成功完成并写入指定 Redis key
- 预期结果2：每个 user 的推荐列表长度为 TopN（不足则按实际）

#### 场景: 与真实数据库联动
作业通过 JDBC 读取 `docker` profile 的 MySQL `user_actions`，训练并写入 Redis。
- 预期结果：Redis 中产生与 DB 行为一致的推荐结果

### 需求: 后端优先读取 ALS 推荐
**模块:** backend/recommendation
后端推荐接口在开关开启时优先从 Redis 读取 ALS 推荐结果，未命中或关闭则继续使用规则推荐。

#### 场景: 开关开启且命中 ALS
Redis 存在 `rec:user:{userId}`，接口返回结果按 ALS 推荐顺序展示（并补齐视频详情/统计字段）。
- 预期结果：接口响应中视频顺序与 Redis 推荐一致（分页规则按实现约定）

#### 场景: 开关关闭或未命中 ALS（fallback）
Redis 不存在或数据为空，接口返回规则推荐结果。
- 预期结果：接口仍可正常返回（不报错，不依赖 ALS）

## 风险评估
- **风险:** 数据稀疏/冷启动导致推荐质量低  
  **缓解:** 设定最小交互阈值；Redis 未命中则回退规则推荐
- **风险:** Redis 内存占用增长  
  **缓解:** TopN 有上限；设置 TTL；可按用户活跃度筛选产出
- **风险:** 离线训练耗时/资源  
  **缓解:** 默认小参数（rank/iter）；支持采样/时间窗口；不做自动调参

