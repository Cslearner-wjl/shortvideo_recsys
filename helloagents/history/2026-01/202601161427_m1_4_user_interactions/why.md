# 变更提案：M1.4 用户互动与行为采集（play/like/comment/favorite）

目录：`helloagents/plan/202601161427_m1_4_user_interactions/`

---

## 目标

- 在不引入大数据组件、不过度设计评论结构的前提下，补齐 M1.4：用户互动（播放/点赞/收藏/评论）与行为采集。
- 为后续推荐与运营统计提供“可落库、可回放、可汇总”的行为数据（`user_actions`）与视频侧计数（`video_stats`）。

---

## 范围

### 范围内
- 新增互动接口：
  - 播放：写 `user_actions`，更新 `video_stats.play_count`
  - 点赞：写/删 `video_likes`，更新 `video_stats.like_count`，并写 `user_actions`
  - 收藏：写/删 `video_favorites`，更新 `video_stats.favorite_count`，并写 `user_actions`
  - 评论：写 `comments`，更新 `video_stats.comment_count`，并写 `user_actions`（type=comment）
- 统一返回结构：`ApiResponse(code,message,data)`
- 计数更新需线程安全：DB 原子自增/自减（`SET cnt = cnt + 1`）
- 测试至少覆盖 1 条完整链路：登录 → 点赞 → 计数变化

### 范围外
- 不接入 Kafka/Flume/Spark 等大数据链路
- 不做复杂评论结构（楼中楼/@ 提及/审核/敏感词等）
- 不做计数强一致的跨表校验任务（如定期对账），仅保证单次请求内事务一致性

---

## 成功标准（可验证）

- 互动接口在并发下计数不丢失（通过 DB 原子更新保障）。
- 点赞/取消点赞、收藏/取消收藏具备幂等行为：重复请求不会导致计数反复增减。
- 所有写操作要求已登录（JWT），冻结用户写操作被禁止（沿用现有 `JwtAuthFilter` 逻辑）。
- 集成测试通过：`/api/auth/*` 注册登录成功后，`POST /api/videos/{id}/like` 生效，且 `video_stats.like_count` 变化正确。

