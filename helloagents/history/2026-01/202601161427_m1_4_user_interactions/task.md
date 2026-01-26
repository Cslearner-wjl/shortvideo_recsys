# 任务清单：M1.4 用户互动与行为采集

目录：`helloagents/plan/202601161427_m1_4_user_interactions/`

---

## 1. 后端接口与数据写入
- [√] 1.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/video/` 新增互动 Controller/Service，提供 play/like/favorite/comment 接口
- [√] 1.2 新增 `video_stats` / `video_likes` / `video_favorites` / `comments` / `user_actions` 的 Entity + Mapper（MyBatisPlus）
- [√] 1.3 在 DB 层实现原子计数更新（`SET cnt = cnt + 1` / `SET cnt = GREATEST(cnt-1,0)`）
- [√] 1.4 调整 Spring Security：`/api/videos/**` 仅 GET 匿名，写接口需登录
- [√] 1.5 视频创建时初始化 `video_stats` 行（避免计数更新缺行）

## 2. 测试
- [√] 2.1 扩展 `backend/src/test/resources/schema-h2.sql`：补齐 videos 与互动相关表，保证 test profile 可跑
- [√] 2.2 新增集成测试：登录 → 点赞 → 断言 `video_stats.like_count` 变化

## 3. 安全检查
- [√] 3.1 检查是否引入敏感信息、是否放开写接口匿名访问、是否存在负计数或并发丢失更新

## 4. 文档更新（知识库）
- [√] 4.1 更新 `helloagents/wiki/api.md`：补充 M1.4 接口
- [√] 4.2 更新 `helloagents/wiki/data.md`：补充表与字段约定（action_type）
- [√] 4.3 更新 `helloagents/wiki/modules/backend.md`：补充互动模块说明
- [√] 4.4 更新 `helloagents/CHANGELOG.md`：记录 M1.4 变更
