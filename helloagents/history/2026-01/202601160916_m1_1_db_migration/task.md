# 任务清单: 数据库建模与迁移（Flyway）

目录: `helloagents/plan/202601160916_db_migration/`

---

## 1. 迁移工具集成（backend）
- [√] 1.1 在 `backend/pom.xml` 增加 Flyway 依赖（仅新增迁移工具相关依赖）
- [√] 1.2 在 `backend/src/main/resources/application.yml` 补齐 `docker` profile 的 Flyway 配置（locations、baseline 等）并保持 `local` 默认不强制连库

## 2. 迁移脚本（db/migration）
- [√] 2.1 新增 `backend/src/main/resources/db/migration/V1__init_schema.sql`：建表与索引
- [√] 2.2 新增 `backend/src/main/resources/db/migration/V2__seed_admin.sql`：插入 `admin_users` 默认记录（bcrypt）
- [√] 2.3 新增 `backend/src/main/resources/db/migration/V3__seed_admin_compat.sql`：增量修正默认管理员记录（兼容 MySQL 新语法提示）

## 3. 文档同步
- [√] 3.1 更新 `docs/数据字典草案.md`：补充字段与索引口径（从“占位”升级为可执行口径）
- [√] 3.2 更新 `docs/接口规范草案.md`：补充鉴权/密码存储约定（不实现接口）
- [√] 3.3 更新 `helloagents/wiki/data.md`：同步表结构与 ER 图（以代码/迁移为准）
- [√] 3.4 更新 `helloagents/wiki/modules/backend.md`：记录迁移工具与数据层能力
- [√] 3.5 更新 `helloagents/CHANGELOG.md`：记录迁移落地

## 4. 验证
- [√] 4.1 验证脚本语法（Flyway 可加载、SQL 可执行）
- [√] 4.2 启动后端（`docker` profile）并在日志中确认迁移成功（如环境可用）
  > 备注: 首次启动迁移到 v2，随后增量迁移到 v3；连接参数已包含 `allowPublicKeyRetrieval=true` 以兼容 MySQL 8 默认认证方式。
- [√] 4.3 提供 SQL 验证语句

## 5. 安全检查
- [√] 5.1 确认未提交明文真实密码/密钥；仅保留示例明文与哈希生成方式
