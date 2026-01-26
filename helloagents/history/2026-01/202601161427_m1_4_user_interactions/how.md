# 技术设计：M1.4 用户互动与行为采集

目录：`helloagents/plan/202601161427_m1_4_user_interactions/`

---

## 表使用方式（写入路径）

### 1) `user_actions`（行为事实表，追加写）
- 用途：沉淀用户行为日志，后续可用于规则推荐/离线训练/统计汇总。
- 写入：每个互动接口在“状态实际发生变化”时写一条（点赞成功、取消成功、收藏成功、评论成功、播放记录）。
- 字段约定：
  - `action_type`：`PLAY` / `LIKE` / `UNLIKE` / `FAVORITE` / `UNFAVORITE` / `COMMENT`
  - `action_time`：服务端当前时间（避免客户端伪造）
  - `duration_ms`、`is_completed`：仅 `PLAY` 使用（可选）

### 2) `video_likes`、`video_favorites`（关系表，去重）
- 用途：表示用户对视频的当前状态（是否点赞/是否收藏）。
- 约束：`(video_id, user_id)` 唯一键，保证去重与幂等。

### 3) `comments`（评论表，最简结构）
- 用途：记录评论内容与创建时间。
- 约束：仅支持一级评论（不含 parent_id / reply_to 等）。

### 4) `video_stats`（计数表）
- 用途：存放视频侧可读的聚合计数（播放/点赞/收藏/评论）。
- 行生成：视频创建时插入 1 行；若历史数据缺失，计数更新前尝试“确保存在”（插入失败则忽略）。

---

## 并发更新策略（计数一致性）

### 原子计数更新（推荐）
- `UPDATE video_stats SET like_count = like_count + 1 WHERE video_id = ?`
- `UPDATE video_stats SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END WHERE video_id = ?`
- 原因：避免读-改-写导致的丢失更新；由数据库单语句保障并发安全。

### 幂等与计数一致性
- 点赞/收藏：
  - 先写关系表（insert/delete），以“影响行数”判断是否状态变化
  - 仅在状态变化时更新计数与写 `user_actions`
- 播放/评论：
  - 直接写事实与计数自增（不要求幂等）

---

## 接口清单（统一响应：`ApiResponse`）

### 1) 播放
- `POST /api/videos/{id}/play`
  - 请求：`{ "durationMs": 12345, "isCompleted": true }`（字段可选）
  - 响应：`ApiResponse<Void>`

### 2) 点赞
- `POST /api/videos/{id}/like`
  - 响应：`ApiResponse<Void>`
- `DELETE /api/videos/{id}/like`
  - 响应：`ApiResponse<Void>`

### 3) 收藏
- `POST /api/videos/{id}/favorite`
  - 响应：`ApiResponse<Void>`
- `DELETE /api/videos/{id}/favorite`
  - 响应：`ApiResponse<Void>`

### 4) 评论
- `POST /api/videos/{id}/comments`
  - 请求：`{ "content": "..." }`
  - 响应：`ApiResponse<Void>`

---

## 事务边界（Transactional）

每个写接口以“单视频、单用户”为事务粒度：
- 点赞/取消点赞、收藏/取消收藏：关系表变更 + 计数更新 + `user_actions` 记录（同一事务）
- 评论：写 `comments` + 计数更新 + `user_actions`（同一事务）
- 播放：写 `user_actions` + 计数更新（同一事务）

失败策略：
- 任何步骤失败整体回滚，避免“关系表成功但计数未变/行为未记”的不一致。

---

## 安全与鉴权

- `/api/videos/**` 保留读接口匿名访问，但写接口必须登录：
  - 通过 Spring Security 按 HTTP Method + Path 细化放行规则（只放行 GET）
- 冻结用户写操作：复用现有 `JwtAuthFilter` 的“写方法拦截”逻辑。

---

## 测试点（最小覆盖）

- ✅ 登录成功（沿用现有注册/登录逻辑）
- ✅ 点赞成功：返回 `code=0`
- ✅ 计数变化：`video_stats.like_count` 从 0 → 1
- ✅ 幂等：重复点赞不继续 +1；重复取消不继续 -1（可选扩展）

