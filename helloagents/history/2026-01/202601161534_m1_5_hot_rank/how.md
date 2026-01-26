# 技术设计：M1.5 热门榜单（热度计算 + Redis 缓存）

目录：`helloagents/plan/202601161534_m1_5_hot_rank/`

---

## 技术方案

### 1) 热度公式（可配置）

采用 **log1p** 平滑（避免头部极端值导致排序“碾压”）：

```
hot_score =
  w_play     * log1p(play_count) +
  w_like     * log1p(like_count) +
  w_comment  * log1p(comment_count) +
  w_favorite * log1p(favorite_count)
```

可选扩展（本期不强制启用）：
- `is_hot` 人工加权 boost（用于活动/运营置顶的“轻增强”）
- 简单时间衰减（如 `score / (1 + ageHours * decay)`），默认关闭

### 2) 刷新策略

**定时刷新（必做）：**
- `@Scheduled` 固定延迟执行，默认每 `5m` 刷新一次（可配置）
- 刷新动作：
  1) 选出审核通过视频集合
  2) 读取/补齐 `video_stats`（缺失则插入初始行）
  3) 计算并写回 `video_stats.hot_score`
  4) 取 TopN（可配置）写入 Redis ZSET

**触发条件（可选，不依赖 Spark）：**
- 后续可在 play/like/comment/favorite 时异步触发“单视频热度重算 + ZSET update”，作为优化项

### 3) Redis Key 设计

- ZSET：`hot:videos`
  - member：`videoId`（字符串）
  - score：`hot_score`（double）
  - 内容：仅存 TopN（`app.hot-rank.topn`）

可选扩展（本期不实现）：
- 分桶榜单：`hot:videos:{yyyyMMdd}`、`hot:videos:{tag}` 等

### 4) 接口返回结构

`GET /api/rank/hot?page=1&pageSize=20`

响应 `ApiResponse<PageResponse<HotRankVideoDto>>`：

- `total`: ZSET 基数（TopN 的可分页总数）
- `page` / `pageSize`
- `items[]`（每项包含基础信息 + 统计字段）：
  - `id/title/description/uploaderUserId/videoUrl/auditStatus/isHot/createdAt`
  - `playCount/likeCount/commentCount/favoriteCount/hotScore`

### 5) 参数配置方式（application.yml）

在 `app.hot-rank` 下提供：
- `enabled`：是否启用
- `refresh-interval-ms`：刷新间隔（毫秒，默认 `300000` = 5 分钟）
- `topn`：ZSET 只缓存 TopN
- `weights.play/like/comment/favorite`：权重

并在 `docker` profile 配置 `spring.data.redis.host/port`（默认读取环境变量）。

---

## ADR（本期无重大架构决策）

本期不引入新链路（Spark/Streaming），仅基于现有 MySQL + Redis 完成榜单能力；属于“读多写少”的缓存型能力，风险可控。

---

## 安全与性能

- Redis/DB 连接信息仅从环境变量/本地配置读取，禁止写入仓库明文密钥。
- 接口为公开只读 GET，按 Spring Security 放行 `/api/rank/**` 的 GET；刷新接口（如提供）限制在 `/api/admin/**`。
- 刷新任务默认只写 TopN 到 Redis；后续可优化为增量更新或分片扫描。

---

## 测试与验收

- 单元测试：热度计算（公式与参数）
- 集成测试：构造数据 → 手动触发 refresh → `GET /api/rank/hot` 顺序与字段验证
- 验收脚本：SQL 创建数据 → 调用 refresh → 查询榜单接口输出
