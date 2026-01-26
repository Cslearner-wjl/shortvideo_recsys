# 技术设计: 批量导入视频脚本

## 技术方案
### 核心技术
- Bash + curl
- 复用后端管理端上传与审核接口
- Flyway 迁移预置 uploaduser

### 实现要点
- 脚本解析 CSV/JSON，按行上传视频
- 通过 Basic Auth 调用 `/api/admin/videos` 上传并返回 videoId
- 可选自动审核：调用 `/api/admin/videos/{id}/audit`
- 提供可配置参数（base URL、账号、输入文件、是否自动审核）

## API设计
### POST /api/admin/videos
- **请求:** multipart 表单（uploaderUserId/title/description/tags/video）
- **响应:** ApiResponse<VideoDto>

### PATCH /api/admin/videos/{id}/audit
- **请求:** JSON `{ "status": "APPROVED" }`
- **响应:** ApiResponse<Void>

## 数据模型
```sql
-- Flyway 迁移增加 uploaduser
INSERT INTO users (username, phone, email, password_hash, status, created_at, updated_at)
VALUES ('uploaduser', '13900000000', 'uploaduser@example.com', '<bcrypt>', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
  status = VALUES(status),
  updated_at = VALUES(updated_at);
```

## 安全与性能
- **安全:** 仅在 docker/test profile 启用；脚本支持通过环境变量覆盖 Basic Auth 与弱口令
- **性能:** 支持批量执行；可选并发优化但先实现串行以保证稳定性

## 测试与部署
- **测试:** 提供脚本干跑/单条上传验证，后端日志确认
- **部署:** 无需额外部署，启动后端 docker/test 后执行脚本
