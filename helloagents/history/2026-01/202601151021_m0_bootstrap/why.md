# 变更提案: M0 工程骨架与本地一键启动

## 需求背景
当前仓库以目录与需求文档为主（`backend/`、`frontend/`、`bigdata/`、`deploy/` 多为空目录），缺少可运行的工程骨架与可复现的本地启动方式，导致后续 M1/M2/M3/M4 的功能迭代无法快速验证与协作。

本变更聚焦里程碑 **M0：工程骨架与环境可跑**，提供“最小可运行闭环”：
- 后端健康检查 API 可启动
- 前端空页面可启动
- 基础依赖（MySQL/Redis/MinIO/Kafka）可一键启动（docker compose）

## 产品分析

### 目标用户与场景
- **用户群体:** 项目研发人员、测试人员
- **使用场景:** 本地开发启动、联调、CI/本地冒烟验证
- **核心痛点:** 环境搭建成本高、缺少统一启动入口、无法快速验证服务是否正常

### 价值主张与成功指标
- **价值主张:** 让新成员在 10 分钟内完成环境启动并跑通健康检查与前端页面
- **成功指标:**
  - `docker compose up -d` 后依赖服务可用（端口开放、容器健康）
  - `mvn test` 通过，后端可访问健康检查
  - `npm run dev` 可启动前端空页面

### 人文关怀
默认不在仓库提交敏感信息（密码/AccessKey/SecretKey 等）；本地示例配置使用占位/示例值并在 README 中提示替换。

## 变更内容
1. 增加 `deploy/docker-compose.yml`：一键启动 MySQL、Redis、MinIO、Kafka（含必要依赖）。
2. 增加 `backend/` 最小化 SpringBoot 工程骨架（含 MyBatisPlus 依赖与 datasource 预留配置）。
3. 增加 `frontend/` 最小化 Vue3（Vite）脚手架（用户端/管理端空页面）。
4. 补齐 `docs/` 的“接口规范草案”“数据字典草案”占位文档。
5. 增加根 `README.md`：启动命令、健康检查示例、常见问题排查。

## 影响范围
- **模块:** deploy / backend / frontend / docs
- **文件:** 新增为主（M0 初始化）
- **API:** 新增健康检查（最小接口）
- **数据:** 仅为后续预留（暂不执行 DDL）

## 核心场景

### 需求: 本地一键启动
**模块:** deploy
在 Windows+WSL2 环境下，开发者可用单条命令拉起依赖服务，并据 README 完成端口与健康验证。

#### 场景: docker compose 启动依赖
具备 Docker Desktop（WSL2 后端）环境。
- 依赖容器启动成功并可访问（MySQL/Redis/MinIO/Kafka）

### 需求: 后端最小可运行
**模块:** backend
提供可编译、可启动、可验证的 SpringBoot 工程骨架，预留 MyBatisPlus 与 datasource 配置。

#### 场景: 健康检查接口
后端服务启动成功。
- `GET /actuator/health` 返回 UP
- `GET /api/health` 返回 OK（或等价 JSON）

### 需求: 前端空页面可运行
**模块:** frontend
提供用户端与管理端的 Vue3 脚手架，支持 `npm run dev` 启动与页面可访问。

#### 场景: 启动前端开发服务器
Node/npm 环境就绪。
- 前端可启动并打开空页面

## 风险评估
- **风险:** 依赖服务端口冲突（3306/6379/9000/9092 等）
  - **缓解:** README 提供端口清单与冲突排查
- **风险:** Windows 路径/WSL 文件系统差异导致脚本不可用
  - **缓解:** README 提供 Windows/WSL 两套命令（优先 Windows+WSL2）

