# 变更提案: 数据库建模与迁移（M0→M1 前置）

## 需求背景
当前仓库已完成 M0 工程骨架，但尚未建立可执行的数据模型与迁移机制，导致后续 M1（业务闭环：用户/视频/互动/行为/榜单/推荐）无法稳定迭代和验收。

本变更目标是在不引入额外业务代码（不新增 Controller/Service）的前提下，完成：
- 核心业务表建模（含必要索引与约束）
- 迁移工具集成（Flyway 或 Liquibase）
- 初始化管理员账号种子数据

## 变更内容
1. 后端集成 Flyway，并提供 MySQL 迁移脚本（`backend/src/main/resources/db/migration`）。
2. 创建核心表：
   - `users` / `admin_users`
   - `videos` / `video_stats`
   - `comments` / `video_likes` / `video_favorites`
   - `user_actions`
   - `daily_metrics`
3. 插入 1 条 `admin_users` 初始化记录（密码哈希使用 bcrypt）。

## 影响范围
- **模块:** backend / docs / deploy / helloagents（知识库同步）
- **API:** 不新增业务 API（本阶段只做迁移）
- **数据:** 新增表结构与索引；新增 1 条管理员种子数据

## 核心场景

### 需求: 自动迁移
**模块:** backend
服务启动时可自动执行数据库迁移，并在启动日志中可见迁移成功信息。

#### 场景: 首次启动自动建表
MySQL 可用，后端以 `docker` profile 启动并指向 MySQL。
- 启动日志显示 Flyway applied 迁移版本
- 数据库中存在全部核心表

### 需求: 管理员初始化
**模块:** backend
首次迁移后具备可登录的管理员账号记录（仅数据层初始化，不实现登录逻辑）。

#### 场景: 校验管理员记录存在
迁移完成。
- `admin_users` 存在默认账号记录

## 风险评估
- **风险:** 表结构在 M1 迭代中频繁变更
  - **缓解:** 采用版本化迁移脚本（只增量，不改写历史脚本）
- **风险:** 不同环境 MySQL 字符集/时区差异
  - **缓解:** 显式指定 `utf8mb4` 与 `Asia/Shanghai`（连接参数/DDL 默认值）

