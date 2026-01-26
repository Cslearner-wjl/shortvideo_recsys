# deploy

## 目的

提供本地一键启动与环境初始化（容器编排、示例配置）。

## 模块概览

- **职责:** docker-compose、本地依赖（MySQL/Redis/MinIO/Kafka/Flume/HDFS）编排、环境变量示例
- **状态:** 开发中
- **最后更新:** 2026-01-21

---

## 规范（核心场景）

### 需求：本地可运行（M0+）

通过 `deploy/docker-compose.yml` 拉起 MySQL/Redis/MinIO/Kafka（含 Zookeeper）与 HDFS，供后续模块开发与测试复用。

端口约定（默认）：
- MySQL 宿主机端口默认使用 `3307`（映射到容器 `3306`），避免与本机已有 MySQL(`3306`) 冲突
- 其余端口以 `deploy/.env` 为准（示例见 `deploy/.env.example`）

### 需求：Flume 行为日志采集（M3.1）

docker compose 启动 Flume，默认使用 `probablyfine/flume`，配置挂载到 `/opt/flume-config/flume.conf`，agent 名称为 `agent`，可通过 `FLUME_IMAGE` 覆盖。

行为日志文件路径需与后端一致：推荐将 `BEHAVIOR_LOG_PATH` 指向 `deploy/data/behavior/behavior-events.log`，以便 Flume TAILDIR 采集并写入 Kafka。

### 需求：Kafka → HDFS 落地

Flume Kafka Source 消费 `behavior-events`，写入 HDFS（NameNode/DataNode），作为行为日志归档。

实现说明（本地）：
- `deploy/flume/flume-env.sh` 通过追加 Spark 自带 Hadoop 客户端 jar，使 Flume HDFS Sink 可用。
- 需先执行 `bigdata/streaming/bin/ensure_spark.sh` 以准备 Spark 缓存目录（docker-compose 会挂载该目录）。

### 需求：M3.2 Streaming 实时统计联调

- 依赖：Redis + Kafka（可复用本 compose）
- 后端建议配置：
  - `BEHAVIOR_LOG_PATH=deploy/data/behavior/behavior-events.log`（让 Flume 能采集到同一路径）
  - `HOT_RANK_MANAGED_BY_STREAMING=true`（避免后端定时 refresh 覆盖 Streaming 写入）
- Streaming 作业：见 `bigdata/streaming/README.md`

### 需求：敏感配置管理

仅提交 `.env.example` 作为示例；真实密码/密钥由本地 `.env` 或环境变量注入（禁止写入知识库与仓库）。

---

## 依赖

- Docker Desktop（WSL2 后端）
- docker compose

---

## 变更历史（索引）

- `202601151021_m0_bootstrap`：docker compose 一键启动依赖
