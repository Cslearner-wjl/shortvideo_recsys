# 技术设计: M1.3 视频管理与 MinIO 接入

## 技术方案

### MinIO 客户端封装
- `MinioClient` 初始化：`endpoint` + `accessKey` + `secretKey`
- `MinioStorageService`：
  - `ensureBucket(bucket)`
  - `putObject(bucket, key, contentType, InputStream)`
  - `removeObject(bucket, key)`
  - `statObject(bucket, key)`（用于测试/校验）

### 配置项（建议）
通过环境变量覆盖：
- `MINIO_ENDPOINT`（如 `http://127.0.0.1:9000`）
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`
- `MINIO_BUCKET`（如 `shortvideo`）
- （可选）`MINIO_PUBLIC_ENDPOINT`（对外访问地址，用于拼接 URL）

### Bucket 初始化策略
- 启动时检查 bucket 是否存在，不存在则创建（仅在训练/开发环境启用）。
- 失败策略：启动失败（避免后续写入全部失败）或降级为“不可上传”（本方案选择启动失败更安全）。

## 数据库字段使用方式

### videos（既有字段复用）
- `audit_status`：`PENDING` / `APPROVED` / `REJECTED`
- `is_hot`：0/1
- `video_url`：存储对象 key（如 `videos/{videoId}/{uuid}.mp4`）或可访问 URL（本方案优先存 key）
- `cover_url`：如支持封面上传则同上（可选）
- `description`：描述
- `tags`：JSON 标签（可选）

### 可能的迁移（按需）
⚠️ 不确定因素: 需求未强制“驳回原因/软删除”，但实际常用。
- 选项A（默认）：不新增字段，驳回仅写 `audit_status=REJECTED`，删除为硬删除（DB+对象）。
- 选项B：新增 `audit_reason`、`deleted_at` 等字段（需要新增 Flyway 迁移）。

## 权限校验
- **管理端** `/api/admin/**`：最小管理员认证（建议：HTTP Basic 读取 `admin_users` 表）。
- **用户端** `/api/videos/**`：公开可读，但仅返回审核通过内容。
- **冻结用户写操作拦截**：沿用 M1.2 的冻结拦截（对后续互动写接口生效）。

## 接口清单

### 管理端
- `POST /api/admin/videos`：multipart 上传（video 文件必填，metadata：title/description/tags 可选）
- `PATCH /api/admin/videos/{id}/audit`：body `{ "status": "APPROVED|REJECTED" }`
- `PATCH /api/admin/videos/{id}/hot`：body `{ "isHot": true|false }`
- `DELETE /api/admin/videos/{id}`：删除 DB + MinIO 对象

### 用户端
- `GET /api/videos/{id}`：仅审核通过可见
- `GET /api/videos/page?sort=hot|time&page=1&pageSize=20`：仅审核通过

## 异常处理
- MinIO 写入失败：返回 50000，DB 回滚并清理对象（尽力清理）
- 资源不存在：返回 404 类错误码（新增 error code）
- 非法状态流转：返回 40000

## 集成测试（docker-compose MinIO）
- 启动 `deploy/docker-compose.yml` 的 MinIO
- 测试流程：上传→校验对象存在+DB记录→审核通过→用户端分页可见→删除→对象与记录消失
- 可通过环境变量开关执行（避免 CI 无 MinIO 时失败）

