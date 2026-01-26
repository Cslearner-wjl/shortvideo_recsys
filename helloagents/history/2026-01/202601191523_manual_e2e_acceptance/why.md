# 变更提案: 手动验收整套系统（前后端 + 大数据链路）

## 需求背景

当前仓库包含：
- 后端（Java/Spring Boot，含鉴权、视频、互动、热门榜、推荐）
- 前端（用户端/管理端，Vue3/Vite）
- 大数据链路（行为日志 -> Flume -> Kafka；Spark Streaming 实时统计写 Redis；Spark ALS 离线推荐写 Redis）

需要提供一份可按步骤执行、可复现、可定位问题的**端到端手动验收文档**，覆盖“可运行性、核心业务闭环、链路数据可达、缓存与回退逻辑、前后端联调”的完整流程，并给出可复制的命令清单。

## 变更内容

1. 提供一套从“依赖启动 → 后端启动 → 前端启动 → 行为链路 → Streaming → ALS → 接口验证”的验收流程
2. 为每个阶段定义：
   - 前置条件
   - 可复制命令
   - 观测点（日志/接口/Redis/Kafka）
   - 通过标准（可验证的断言）
3. 汇总常见失败点与排查建议（端口、profile、路径挂载、Spark 版本、Redis/Kafka 连接等）

## 影响范围

- **模块:** backend、frontend-user、frontend-admin、deploy、bigdata、scripts
- **文件:** 仅新增方案包文档（不修改业务代码）
- **API:** 不变（仅验收使用）
- **数据:** 仅验收数据（本地 MySQL/Redis），不接触生产环境

## 核心场景

### 需求: 基础可运行性验收
**模块:** deploy / backend / frontend

#### 场景: 一键启动依赖并完成健康检查
前置条件：安装 Docker、JDK17、Maven、Node.js（可选 python3/redis-cli）
- 依赖可正常启动（MySQL/Redis/MinIO/Kafka/Flume）
- 后端 `/api/health` 可访问
- 前端可打开并完成最小联调

### 需求: 业务闭环验收（M1 系列）
**模块:** backend / frontend-user / frontend-admin

#### 场景: 用户注册登录与视频流互动
- 用户完成注册/登录
- 列表/详情可展示审核通过的视频
- 互动行为（play/like/favorite/comment）返回成功并可观察到计数变化

#### 场景: 管理端审核与热门榜
- 管理端可用 Basic Auth 登录
- 视频可完成审核（APPROVED）并在用户端可见
- 热门榜接口可返回数据，必要时可手动刷新

### 需求: 行为链路验收（M3.1）
**模块:** backend / deploy / bigdata

#### 场景: 行为事件从后端落盘并进入 Kafka
- 后端行为日志按 JSON Lines 追加写入指定路径
- Flume taildir 采集该文件并写入 Kafka `behavior-events`
- 可消费 Kafka topic 验证事件到达

### 需求: 实时统计验收（M3.2）
**模块:** bigdata/streaming / backend / deploy / scripts

#### 场景: Streaming 消费 Kafka 并写 Redis 统计
- Spark Streaming 作业启动并持续消费
- Redis 中出现 `stats:video:{videoId}` 与 `hot:videos`
- 后端热门榜接口优先读取 Redis（数据与 Redis 对齐）

### 需求: 离线推荐验收（M4）
**模块:** bigdata/batch / backend / scripts

#### 场景: ALS 批处理写入 Redis 推荐并被后端优先读取
- ALS 作业可从 JDBC 读取 `user_actions` 并写入 `rec:user:{userId}`
- 后端在启用开关时优先按 ALS 顺序返回；未命中回退规则推荐且仍可返回

## 风险评估

- **环境差异风险:** Windows/WSL2 vs Linux；Docker 端口占用与文件挂载差异。
  - 缓解：明确端口、目录、profile、环境变量；提供“检查点 + 排错命令”。
- **大数据运行风险:** Spark 版本与 Java 17 兼容问题（Spark 3.1.x 常见兼容问题）。
  - 缓解：推荐使用仓库脚本固定 Spark 3.5.1 运行。
- **依赖可用性风险:** Kafka/Flume/Redis 未就绪导致链路不通。
  - 缓解：先验收 Kafka topic 消费，再验收 Streaming/Redis。
- **敏感信息风险:** `.env` 中的密码/密钥被误扩散。
  - 缓解：验收文档只引用变量名与示例，不记录真实密钥；不触达生产环境。

