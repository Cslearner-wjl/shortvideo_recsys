# 变更提案: M1.3 视频管理（上传/审核/删除）与 MinIO 接入

## 需求背景
当前系统已具备用户体系与鉴权（M1.2），但缺少内容侧最小闭环：视频上传、审核、热门标记、删除与用户端可见性控制。该能力是“刷视频/榜单/推荐”的内容供给前置条件。

本变更在不引入 CDN、不实现大数据链路、不做分片上传的约束下，落地：
- 管理端：上传到 MinIO + 写入 `videos`（待审）+ 审核/热门/删除
- 用户端：仅展示审核通过的视频详情与分页列表
- MinIO bucket 初始化策略与异常处理
- 集成测试：使用 docker-compose 的 MinIO 跑通上传/审核/删除链路

## 变更内容
1. 增加 MinIO 客户端封装与配置（endpoint/accessKey/secretKey/bucket）。
2. 管理端接口：
   - `POST /api/admin/videos`（multipart 上传，落 MinIO，写 `videos`，`audit_status=PENDING`）
   - `PATCH /api/admin/videos/{id}/audit`（通过/驳回）
   - `PATCH /api/admin/videos/{id}/hot`（设置热门标记）
   - `DELETE /api/admin/videos/{id}`（DB + MinIO）
3. 用户端接口：
   - `GET /api/videos/{id}`（仅审核通过可见）
   - `GET /api/videos/page?sort=...`（仅审核通过）
4. 最小权限校验：管理端接口需管理员认证；用户端接口公开可读（仅返回已审核内容）。
5. 集成测试：docker-compose MinIO 环境下验证对象与 DB 一致性。

## 影响范围
- **模块:** backend / docs / 数据库迁移（如需）
- **API:** 新增 video/admin-video 接口
- **数据:** 使用既有 `videos` 字段存储对象 key/URL；如需要审计原因或软删除则新增迁移

## 核心场景

### 需求: 视频上传落库与落对象存储
**模块:** backend
管理员上传视频文件，服务将文件写入 MinIO，并创建 `videos` 记录（待审）。

#### 场景: 上传成功
- MinIO bucket 中出现对象
- MySQL 中 `videos` 存在记录（`audit_status=PENDING`）

### 需求: 审核与用户可见性
**模块:** backend
审核通过后，用户端分页与详情可读取；待审/驳回不可见。

#### 场景: 审核通过后可见
- `GET /api/videos/page` 可拉取到该视频
- `GET /api/videos/{id}` 返回详情

### 需求: 删除一致性
**模块:** backend
删除后 DB 记录与 MinIO 对象一致（硬删除或软删除需一致）。

#### 场景: 删除后不可见
- 对象被删除（或标记删除后不可访问）
- DB 记录消失（或标记删除后过滤）

## 风险评估
- **风险:** MinIO 与 DB 写入不一致（部分成功）
  - **缓解:** 上传前预生成 object key；失败时回滚并清理对象/记录（尽力而为）
- **风险:** 管理端鉴权未完善
  - **缓解:** 采用最小管理员认证（Basic 或 Header Token），不引入复杂权限框架

