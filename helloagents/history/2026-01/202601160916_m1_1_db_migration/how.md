# 技术设计: 数据库建模与迁移（Flyway）

## 技术方案

### 核心技术
- **迁移工具:** Flyway（Spring Boot 官方集成）
- **数据库:** MySQL 8.x
- **脚本位置:** `backend/src/main/resources/db/migration`

### 迁移脚本编号
- `V1__init_schema.sql`：创建表结构与索引
- `V2__seed_admin.sql`：写入 `admin_users` 初始化记录
- `V3__seed_admin_compat.sql`：增量修正默认管理员记录（兼容 MySQL 新语法提示/终端显示）

## 数据模型（表清单）

> 说明：字段以“可用为先”的 M1 口径建模，后续如新增字段必须新增迁移脚本，不回写旧脚本。

### users
- 基础信息：用户名、手机号、邮箱、状态、密码哈希、创建/更新时间
- 约束：username/email/phone 唯一（允许 phone/email 为空时的唯一约束策略以 MySQL 行为为准）

### admin_users
- 基础信息：用户名、显示名、密码哈希、状态、创建/更新时间
- 种子数据：默认 `admin`

### videos
- 基础信息：上传者、标题、存储 URL（视频/封面）、标签、审核状态、热门标记、创建/更新时间

### video_stats
- 聚合计数：播放/点赞/评论/收藏计数、热度分
- 约束：与 `videos` 一对一（`video_id` 唯一）

### comments
- 评论：视频、用户、内容、创建时间

### video_likes / video_favorites
- 行为关系：视频、用户、创建时间
- 约束：`(video_id, user_id)` 唯一，避免重复点赞/收藏

### user_actions
- 行为流水：用户、视频、行为类型、行为时间、播放时长/完播标记
- 索引：按 user/video + time 查询优化

### daily_metrics
- 日维度统计：日期、播放量、点赞量、评论量、收藏量、活跃用户数、新增用户数等（以列为主，M2 统计可复用）
- 约束：`day` 唯一

## 初始化数据策略
- 仅插入 `admin_users` 1 条记录（避免污染业务数据）。
- 密码存储：bcrypt 哈希（形如 `$2a$...`），明文示例为 `Admin123!`。
- 生成方式（示例，二选一）：
  1) 使用 Spring Security 的 BCryptPasswordEncoder 在本地生成（仅用于生成哈希，不要求项目依赖）。
  2) 使用容器工具生成（如 `docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" 'Admin123!' | tr -d ':\n'`）。

## 验收口径
- 启动日志出现 Flyway 迁移成功（版本 V1/V2 applied）。
- SQL 校验：
  - `SHOW TABLES;`
  - `SELECT COUNT(*) FROM admin_users;`
  - `DESCRIBE users;`
  - `SHOW INDEX FROM user_actions;`
