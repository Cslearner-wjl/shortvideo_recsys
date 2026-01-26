# 任务清单: M0 工程骨架与本地一键启动

目录: `helloagents/plan/202601151021_m0_bootstrap/`

---

## 1. deploy（本地依赖一键启动）
- [√] 1.1 新增 `deploy/docker-compose.yml`：MySQL、Redis、MinIO、Kafka（含必要依赖），并配置卷/端口
- [√] 1.2 新增 `deploy/.env.example`：集中示例配置（端口/账号/密码占位）

## 2. backend（最小化工程骨架）
- [√] 2.1 新增 `backend/pom.xml`：Spring Boot + Web + Actuator + MyBatisPlus + 测试依赖
- [√] 2.2 新增 `backend/src/main/java/...`：启动类与 `GET /api/health`
- [√] 2.3 新增 `backend/src/main/resources/application.yml`：datasource 预留配置（默认不强制连库），以及端口/日志基础配置
- [√] 2.4 新增 `backend/src/test/java/...`：最小单测（Context loads/health endpoint）

## 3. frontend（脚手架）
- [√] 3.1 新增 `frontend/user/`：Vue3 + Vite 最小项目（空页面+基础脚本）
- [√] 3.2 新增 `frontend/admin/`：Vue3 + Vite 最小项目（空页面+基础脚本）

## 4. docs（草案占位）
- [√] 4.1 新增 `docs/接口规范草案.md`：接口规范与通用响应结构（占位）
- [√] 4.2 新增 `docs/数据字典草案.md`：核心实体与字段口径（占位）

## 5. README 与验证
- [√] 5.1 新增根 `README.md`：Windows+WSL2 启动指南、验证命令、健康检查 curl 示例、常见问题排查
- [X] 5.2 执行验证：`docker compose up -d`、`mvn test`、`npm run dev`/`npm run build`（记录结果）
  > 备注: 已验证 `docker compose config` 与 `mvn test`；当前执行环境无法连接 Docker Engine，且无 Node.js 可执行程序，未能完成 `docker compose up -d` 与前端构建/启动验证。

## 6. 安全检查
- [√] 6.1 检查仓库无明文密钥/令牌；示例配置仅占位

## 7. 知识库同步
- [√] 7.1 更新 `helloagents/wiki/*` 对 M0 落地内容的描述（deploy/backend/frontend）
- [√] 7.2 更新 `helloagents/CHANGELOG.md` 记录 M0 初始化
