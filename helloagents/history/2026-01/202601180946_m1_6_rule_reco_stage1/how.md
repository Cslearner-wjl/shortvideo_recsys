# 技术设计：M1.6 规则推荐（阶段1）

目录：`helloagents/plan/202601180946_m1_6_rule_reco_stage1/`

---

## 1) 用户偏好画像计算（基于 user_actions）

### 行为强度权重（默认，可配置）

满足 `favorite > comment > like > play`：

- `FAVORITE`：4
- `COMMENT`：3
- `LIKE`：2
- `PLAY`：1

### 画像计算流程

1. 从 `user_actions` 读取最近 N 条行为（按 `action_time` 倒序，N 可配置）。
2. 去重收集 `video_id`，批量读取对应 `videos.tags`。
3. 将视频 tags 解析为 tag 列表（JSON array / 逗号分隔容错）。
4. 对每条行为按行为强度权重累加到 tag 分数：`tagScore[tag] += actionWeight[type]`。
5. 输出 TopK tags 作为用户偏好画像（K 可配置）。

---

## 2) 候选集生成

为避免依赖数据库 JSON 查询（H2/MySQL 差异），阶段1 使用“候选池 + 内存过滤”：

- 候选池：从 `videos` 中取审核通过的最新 M 条（M 可配置），作为推荐候选。
- 标签候选：在候选池内筛选 tags 命中偏好 TopK 的视频，并按“匹配度”排序：
  - 匹配度：`sum(tagScore[tag] for tag in videoTags ∩ userTopTags)`
- 热门候选：从 `hot:videos`（ZSET）取 TopN（如无缓存则按 `video_stats.hot_score` 降序兜底）。
- 随机候选：从候选池中 shuffle 取若干。

---

## 3) 混排策略（规则）

### 新用户（无行为）

`hotTopN + randomFill`：

- 先取热门 TopN（上限由 `pageSize * hotRatio` 或固定 N 控制）
- 不足部分用随机补齐

### 老用户（有行为）

`tagFirst + hotMix + randomMix`：

- 优先标签候选（按匹配度排序）
- 混入部分热门（避免信息茧房 + 保证供给）
- 混入少量随机（探索）

默认比例（可配置）：

- `tagRatio = 0.7`
- `hotRatio = 0.2`
- `randomRatio = 0.1`

---

## 4) 去重策略

- 从最近 N 条 `user_actions` 中取 `video_id` 作为排除集合
- 推荐时跳过排除集合中的视频
- 备注：阶段1 按“最近 N 条行为”过滤，不做全量历史过滤

---

## 5) 缓存策略（可选）

阶段1 默认不强制引入用户级缓存；如后续需要，可增加：

- `reco:{userId}:{cursor}` → 视频 id 列表（TTL 60s~5min）

---

## 6) API 设计

`GET /api/recommendations`

请求参数：

- `page`（默认 1）
- `pageSize`（默认 20，上限 100）
- `cursor`（可选，数值字符串；存在时优先使用滚动模式，表示 offset）

响应结构：

- `items[]`：推荐视频列表（基础信息 + 统计字段 + tags）
- `nextCursor`：下一页 offset（字符串；若无更多则为 `null`）
- `page/pageSize`：用于兼容分页模式

---

## 7) 测试点

1. 标签解析容错（JSON array / 逗号分隔 / 空值）。
2. 新用户推荐：返回包含热门与随机、且数量满足 `pageSize`（在数据充足时）。
3. 老用户推荐：偏好标签视频优先出现，且排除最近行为视频。
4. 滚动参数：`cursor` 翻页结果稳定（同一 `cursor` 重复请求返回一致顺序）。
5. 单元测试：画像计算与混排去重逻辑的关键分支覆盖。

