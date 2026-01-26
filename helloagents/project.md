# 项目技术约定

> 本文档描述本项目的技术栈、工程结构与协作约定。若与代码实现冲突，以运行时行为为准，并同步更新 `helloagents/wiki/*`。

---

## 技术栈（以代码为准）

- **后端:** Java 17 / Spring Boot 3.2.2（Web + Security）/ MyBatis-Plus 3.5.5 / Flyway
- **数据库与中间件:** MySQL 8.0 / Redis 7 / MinIO（MinIO Java SDK 8.5.17）
- **测试:** Spring Boot Test + MockMvc（`test` profile 使用 H2；MinIO 集成测试使用 `docker` profile 且需本地 MinIO）
- **前端:** Vue 3 + Vite + TypeScript + Element Plus + ECharts
- **容器化:** Docker Compose（开发环境依赖编排）
- **大数据链路（部分落地）:** Flume / Kafka / HDFS / Spark Streaming / Spark MLlib ALS

---

## 仓库结构（monorepo）

- `backend/`：Spring Boot 后端（已落地 M1.2/M1.3/M1.4 的核心能力）
- `frontend/`：前端工程
  - `frontend/user/`：用户端（Vite + Vue 3）
  - `frontend/admin/`：管理端（Vite + Vue 3）
- `deploy/`：docker-compose、环境变量示例与本地依赖编排
- `docs/`：需求/流程/设计等外部文档（作为输入材料，必要时同步到 `helloagents/`）
- `bigdata/`：大数据链路规划与预留目录
- `helloagents/`：知识库（SSOT：`CHANGELOG.md`、`project.md`、`wiki/*`、`history/*`、`plan/*`）

---

## 开发与运行环境（建议）

- **操作系统:** Windows 11 + WSL2（Ubuntu）+ Docker Desktop（WSL2 后端）
- **Java:** JDK 17
- **构建:** Maven
- **Node.js:** 20+（用于前端）

---

## 运行 Profile 约定

- **默认 profile:** `local`（最小启动/健康检查，不连接 DB/Redis）
- **业务验证:** 使用 `docker` 或 `test` profile（启用数据源与业务控制器）
- **注意:** 主要业务控制器/Service 标注 `@Profile("docker","test")`，需显式激活对应 profile 才可用

---

## 测试与运行约定

- **后端单测/集成测:** `backend` 下使用 Spring Boot Test；`test` profile 使用 H2（见 `backend/src/test/resources/application-test.yml` 与 `schema-h2.sql`）
- **MinIO 集成测试:** 需要本地启动 MinIO（参考 `deploy/docker-compose.yml`），并设置环境变量 `RUN_MINIO_IT=true`
- **前端单测:** `frontend/user` 与 `frontend/admin` 使用 Vitest，命令 `npm run test:unit`
- **前端 E2E:** Playwright（Chromium），用户端默认 4173、管理端 4174，命令 `npm run test:e2e`
- **压测:** JMeter 计划文件 `tests/jmeter/backend_api.jmx`，容器内压测目标使用 `host.docker.internal:18080`

---

## 质量与安全

- **敏感信息:** 禁止提交明文密钥/令牌/连接串；使用 `.env.example` 提供示例并通过环境变量注入
- **数据合规:** 用户与行为数据属于敏感业务数据，采用最小权限与必要脱敏策略（按需落地）
- **接口一致性:** 文档与实现不一致时，以代码为准并更新 `helloagents/wiki/api.md`、`helloagents/wiki/data.md`

