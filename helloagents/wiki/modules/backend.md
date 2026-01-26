# backend

## 目的

承载后端 REST API 与业务逻辑，连接 MySQL/Redis/MinIO，并对接推荐与统计结果。

## 模块概览

- **职责:** 用户与鉴权、视频管理、互动行为采集、统计计数、管理端能力（部分规划）
- **状态:** 开发中（已落地 M1.2/M1.3/M1.4/M1.5/M1.6）
- **运行 profile:** docker/test（local 仅最小启动与健康检查）
- **最后更新:** 2026-01-22

---

## 规范（核心场景）

### 需求：用户与鉴权

支持注册/登录/查询当前用户与资料更新；管理员账号独立认证。

#### 场景：注册/登录/JWT（M1.2）

使用邮箱验证码注册；登录返回 JWT；`GET /api/users/me` 需要携带 JWT；冻结用户禁止登录与写操作（按代码约定执行）。

#### 场景：个人资料维护

- `PUT /api/users/me` 支持昵称/手机/邮箱/头像/简介更新
- `POST /api/users/me/password` 支持修改登录密码

### 需求：视频服务

支持管理端上传、审核、删除、热门标记；用户端仅展示审核通过的视频。

#### 场景：管理端视频管理（M1.3）

上传视频落 MinIO（bucket 自动初始化），写入 `videos`（待审）；审核通过后用户端列表/详情可见；删除同步删除对象存储与数据库记录。

#### 场景：批量导入准备（uploaduser 预置）

- docker/test 环境通过 Flyway 预置 `uploaduser` 账号（用于脚本指定 `uploaderUserId`）
- 迁移会覆盖 `uploaduser` 的密码为默认口令（仅用于测试/演示）
- 实际上传仍通过 `/api/admin/videos`，审核通过后可被用户端播放

#### 场景：用户端列表/详情互动状态回显

- `GET /api/videos/{id}` 与 `GET /api/videos/page` 返回 `liked`/`favorited` 字段（未登录时默认 false）
- `GET /api/recommendations` 同步返回 `liked`/`favorited`，用于前端点赞/收藏切换

### 需求：行为采集与计数

记录播放/点赞/评论/收藏等行为，并维护 `video_stats` 聚合计数。

#### 场景：互动与计数更新（M1.4）

- 写入 `user_actions`（事实表，`action_type` 见 `helloagents/wiki/data.md`）
- 点赞/收藏使用关系表（`video_likes`/`video_favorites`）保证幂等
- `video_stats` 使用数据库原子自增/自减，避免并发丢失更新
- 并发注意：避免“同一事务内先尝试插入 `video_stats` 再更新同一行计数”的模式；高并发下可能触发 InnoDB S->X 锁升级死锁（已在压测稳定性修复中处理）
- 行为事件同步写入 JSON Lines 日志（`app.behavior-log.path`），供 Flume -> Kafka 采集

### 需求：评论列表与点赞

新增评论列表接口并支持评论点赞，用于前端展示与后续热度扩展。

#### 场景：评论列表与点赞状态

- `GET /api/videos/{id}/comments` 返回分页评论与点赞数
- `POST/DELETE /api/comments/{id}/likes` 更新评论点赞数
- 管理端可通过 `/api/admin/videos/{id}/comments` 获取扩展字段
 - 评论列表附带用户基础信息（含头像）

### 需求：热门榜单（M1.5）

基于 `video_stats` 的聚合计数计算 `hot_score`，并使用 Redis ZSET 缓存热门排行：

- 定时任务每 N 分钟刷新 `video_stats.hot_score`，并写入 `hot:videos`
- `GET /api/rank/hot` 匿名只读分页返回热门视频（基础信息 + 统计字段）

配置与验收：

- 配置：`backend/src/main/resources/application.yml`（`app.hot-rank.*`、`spring.data.redis.*`）
- 验收脚本：`scripts/acceptance_hot_rank.ps1`

#### 场景：M3.2 Streaming 管理热门榜（实时）

当 `app.hot-rank.managed-by-streaming=true` 时：

- Spark Streaming 作业维护 Redis：
  - `stats:video:{videoId}`（Hash，含计数与 `hot_score`）
  - `hot:videos`（ZSET，score=hot_score）
- 后端 `/api/rank/hot` 优先从 Redis Hash 读取统计字段（Redis 缺失时回退 DB）
- 后端定时 refresh 不执行（避免覆盖 Streaming 写入的 ZSET）

### 需求：规则推荐（M1.6，阶段1）

基于 `user_actions` 的规则推荐入口：

- 新用户：热门 TopN + 随机补齐
- 老用户：按行为强度统计 tags 偏好（`favorite>comment>like>play`），优先推荐同标签；混入部分热门与随机
- 去重：过滤最近 N 条行为中的视频，避免推荐已看过/强互动内容

配置与验收：

- 配置：`backend/src/main/resources/application.yml`（`app.recommendations.*`）
- Demo：`scripts/demo_recommendations.ps1`

### 需求：管理后台接口（M2.1）

提供用户管理与看板统计接口，统一通过 `/api/admin/**` 管理端入口访问（Basic Auth）。

#### 场景：用户管理

- 用户列表支持分页与关键字查询（账号/用户名/手机号/邮箱）
- 支持冻结/解封用户（影响登录与写操作）

#### 场景：看板统计

- 每日播放趋势、用户增长、活跃用户、视频发布量与热门 TopN 聚合统计

#### 场景：管理员账号管理

- 管理员账号增删改与改密（`/api/admin/admins`）

---

## API 与数据

- **API:** `helloagents/wiki/api.md`
- **数据模型:** `helloagents/wiki/data.md`（以 Flyway 脚本为准）

---

## 依赖

- MySQL / Redis / MinIO
- Flyway（`backend/src/main/resources/db/migration`）
- （规划）Kafka/Spark（接入大数据链路后）

## 测试

- 单元测试: `mvn -Dtest=HotScoreCalculatorTest,HealthControllerTest,VideoTagsParserTest test`
- 集成测试: `RUN_MINIO_IT=true mvn -Dtest=*IntegrationTest,BackendApplicationTests test`
- 测试报告: `docs/test/backend/surefire-test-summary.md`、`docs/test/backend/permissions-validation-report.md`、`docs/test/backend/recommendation-rank-report.md`、`docs/test/backend/video-interaction-report.md`
- 压测: `tests/jmeter/backend_api.jmx`（报告输出 `docs/tests/jmeter/`）

---

## 变更历史（索引）

- `202601210936_batch_video_import`：批量导入脚本联动与 uploaduser 预置
- `202601202011_interaction_state_fix`：评论一致性与点赞/收藏状态回显
- `202601151021_m0_bootstrap`：M0 工程骨架与健康检查
- `202601160916_m1_1_db_migration`：M1.1 数据库建模与迁移
- `202601161034_m1_2_auth_jwt`：M1.2 注册登录 + JWT
- `202601161205_m1_3_video_minio`：M1.3 视频管理 + MinIO
- `202601161427_m1_4_user_interactions`：M1.4 互动与行为采集
- `202601161534_m1_5_hot_rank`：M1.5 热门榜单（热度计算 + Redis 缓存）
- `202601180946_m1_6_rule_reco_stage1`：M1.6 规则推荐（阶段1）
- `202601181052_m2_1_admin_api`：M2.1 管理后台接口（用户管理/看板）
