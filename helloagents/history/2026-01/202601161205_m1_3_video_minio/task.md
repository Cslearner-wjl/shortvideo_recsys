# 任务清单: M1.3 视频管理与 MinIO 接入

目录: `helloagents/plan/202601161205_m1_3_video_minio/`

---

## 1. 依赖与配置
- [√] 1.1 `backend/pom.xml` 增加 MinIO Java SDK 依赖
- [√] 1.2 `backend/src/main/resources/application.yml` 增加 MinIO 配置项（endpoint/ak/sk/bucket）

## 2. MinIO 封装与 bucket 初始化
- [√] 2.1 新增 `MinioStorageService`（put/remove/stat/ensureBucket）
- [√] 2.2 启动时初始化 bucket（不存在则创建）

## 3. 数据层
- [√] 3.1 新增 `VideoEntity` / `VideoMapper`（复用 `videos` 表字段）
- [√] 3.2 （如选项B）新增迁移脚本：审计原因/软删除字段（按需决定）
  > 备注: 当前按选项A落地（不新增字段）；删除为硬删除（DB+MinIO）。

## 4. 权限与接口实现
- [√] 4.1 管理端鉴权：`/api/admin/**` 使用 Basic 认证读取 `admin_users`（最小实现）
- [√] 4.2 实现 `POST /api/admin/videos`：multipart 上传到 MinIO + 写 `videos`（PENDING）
- [√] 4.3 实现审核/热门/删除接口（DB + MinIO 一致性）
- [√] 4.4 实现用户端 `GET /api/videos/{id}` 与分页接口（仅 APPROVED）

## 5. 文档与知识库
- [√] 5.1 更新 `docs/接口规范草案.md`（视频相关接口与鉴权说明）
- [√] 5.2 更新 `docs/数据字典草案.md`（videos 字段口径与状态枚举）
- [√] 5.3 同步 `helloagents/wiki/api.md`、`helloagents/wiki/data.md`、`helloagents/wiki/modules/backend.md`
- [√] 5.4 更新 `helloagents/CHANGELOG.md`

## 6. 测试与验收
- [√] 6.1 增加集成测试（MinIO + DB）：上传/审核/删除链路
- [√] 6.2 在 docker-compose MinIO 环境下跑通测试并记录结果
  > 备注: `backend/src/test/java/.../VideoMinioIntegrationTest` 已跑通（RUN_MINIO_IT=true）。

## 7. 安全检查
- [√] 7.1 检查 MinIO ak/sk、JWT secret 等均支持环境变量覆盖，禁止提交真实密钥
