# 任务清单: M2.1 管理后台接口（用户管理/看板）

目录: `helloagents/plan/202601181052_m2_1_admin_api/`

---

## 1. 用户管理接口
- [√] 1.1 新增管理端用户控制器与 DTO（`backend/src/main/java/com/shortvideo/recsys/backend/admin/AdminUserController.java` 等），验证 why.md#需求-用户管理-场景-查询用户列表
- [√] 1.2 实现 GET `/api/admin/users` 分页与关键词查询（`UserMapper` + `QueryWrapper`），验证 why.md#需求-用户管理-场景-查询用户列表
- [√] 1.3 实现 PATCH `/api/admin/users/{id}/status` 冻结/解封逻辑（状态仅 0/1），验证 why.md#需求-用户管理-场景-冻结解封用户

## 2. 看板统计接口
- [√] 2.1 新增看板控制器/服务/Mapper 与 DTO（`AdminAnalyticsController/AdminAnalyticsService/AdminAnalyticsMapper`），验证 why.md#需求-看板统计
- [√] 2.2 实现每日播放聚合接口 `daily-play`（SQL 聚合 `user_actions`），验证 why.md#需求-看板统计-场景-每日播放趋势
- [√] 2.3 实现用户增长接口 `user-growth`（SQL 聚合 `users`），验证 why.md#需求-看板统计-场景-用户增长
- [√] 2.4 实现热门 TopN 接口 `hot-topn`（SQL 聚合 `video_stats` + `videos`），验证 why.md#需求-看板统计-场景-热门-topn
- [√] 2.5 实现活跃用户接口 `active-users`（SQL 聚合 `user_actions` 去重），验证 why.md#需求-看板统计-场景-活跃用户

## 3. 安全检查
- [√] 3.1 权限校验与输入验证（分页、时间区间、TopN、状态值），按 G9 执行安全检查

## 4. 文档更新
- [√] 4.1 更新 `helloagents/wiki/api.md` 增加管理后台接口说明
- [√] 4.2 更新 `helloagents/wiki/modules/backend.md` 记录模块规范与变更历史

## 5. 测试
- [√] 5.1 更新 `backend/src/test/resources/schema-h2.sql` 补齐 `admin_users` 表
- [√] 5.2 新增集成测试：用户查询 + 冻结/解封（`backend/src/test/java/com/shortvideo/recsys/backend/admin/AdminUserIntegrationTest.java`）
- [√] 5.3 新增集成测试：看板统计接口（`backend/src/test/java/com/shortvideo/recsys/backend/admin/AdminAnalyticsIntegrationTest.java`）
