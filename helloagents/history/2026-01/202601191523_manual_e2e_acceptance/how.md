# 技术设计: 手动验收整套系统（前后端 + 大数据链路）

## 技术方案

### 核心原则

- **先依赖、后服务、再链路**：依赖（MySQL/Redis/MinIO/Kafka/Flume）稳定后再启动后端；后端稳定后再启动前端与大数据作业。
- **端到端分段验收**：每段都定义“可复制命令 + 观测点 + 通过标准”，避免一次性跑全链路难排错。
- **以仓库脚本为主入口**：优先复用 `scripts/` 与 `backend/` 中已有启动/验收脚本，减少手工差异。

### 组件与端口约定（以仓库脚本与 compose 为准）

- 后端：`http://localhost:18080`（建议统一用 18080 便于脚本复用）
- MySQL：`127.0.0.1:3307`
- Redis：`127.0.0.1:6379`
- Kafka（对外）：`localhost:9093`（docker compose 对外端口）
- Kafka（容器内）：`kafka:9092`
- Flume：taildir 采集 `deploy/data/behavior/behavior-events.log` 写入 Kafka `behavior-events`

---

## 验收步骤（推荐顺序）

> 说明：以下命令均在仓库根目录执行，除非特别说明。

### 0. 先决条件检查

建议具备：
- Docker / Docker Compose
- JDK 17 + Maven
- Node.js 20+
- 可选：`python3`（脚本解析 JSON）、`redis-cli`（更方便观察 Redis）

快速确认：

```bash
docker version
docker compose version
java -version
mvn -v
node -v
```

---

### 1. 启动依赖（MySQL/Redis/MinIO/Kafka/Flume）

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d mysql redis minio zookeeper kafka flume
docker compose --env-file deploy/.env -f deploy/docker-compose.yml ps
```

通过标准：
- mysql/redis/kafka/flume 容器均为 running（mysql 健康检查为 healthy）

观测点（可选）：

```bash
docker logs --tail=200 shortvideo-recsys-kafka
docker logs --tail=200 shortvideo-recsys-flume
```

---

### 2. 启动后端（docker profile，端口 18080）

推荐：使用仓库脚本（自动设置 env 与 profile）

```bash
./backend/run_backend_18080.sh
```
```
cd /home/wjl/workspace/shortvideo_recsys/backend
mvn spring-boot:run -Dspring-boot.run.profiles=docker -Dspring-boot.run.jvmArguments="-Dserver.port=18080"
```

通过标准：

```bash
curl -s http://localhost:18080/api/health
```

关键约束（必须）：
- `SPRING_PROFILES_ACTIVE=docker`
- 数据源连向 `127.0.0.1:3307`，Redis 连向 `127.0.0.1:6379`

---

### 3. 前端验收（用户端 + 管理端）

#### 3.1 启动用户端

```bash
cd frontend/user
npm install
npm run dev
```

联调说明：
- 默认通过 Vite proxy 转发 `/api`
- 可用 `VITE_PROXY_TARGET=http://localhost:18080` 显式指向后端

#### 3.2 启动管理端

```bash
cd frontend/admin
npm install
npm run dev
```

管理端鉴权：
- 使用 Basic Auth 调用 `/api/admin/**`
- 后端脚本会设置 `APP_ADMIN_BOOTSTRAP_RESET_PASSWORD=true`，确保存在可用账号（以代码/配置为准）

通过标准（手动）：
- 页面可打开
- 登录后可进入列表/看板
- 操作失败时有明确提示（网络/401/500）

---

### 4. 基础业务闭环验收（建议先用脚本，后用 UI 复验）

#### 4.1 规则推荐演示（Windows PowerShell 可选）

脚本：`scripts/demo_recommendations.ps1`
- 会自动起 compose（可选参数），自动注册用户、写入演示数据并请求推荐接口
- 注意：默认 `BaseUrl` 可能不是 18080，运行前请对齐参数

示例（PowerShell）：

```powershell
pwsh -File scripts/demo_recommendations.ps1 -BaseUrl "http://127.0.0.1:18080" -EnvFile "deploy/.env" -StartCompose
```

通过标准：
- `/api/recommendations` 返回非空列表
- 多次触发行为后，推荐结果可发生变化或至少稳定可返回

---

### 5. 行为日志落盘验收（M3.1 前置）

目标：让后端行为日志写入到 Flume 采集路径（与 compose 挂载一致）

推荐设置：

```bash
export APP_BEHAVIOR_LOG_PATH=deploy/data/behavior/behavior-events.log
```

说明：
- 后端会按 JSON Lines 追加写入该文件
- Flume taildir 采集后写 Kafka `behavior-events`

通过标准：
- 文件持续增长（有新行）
- Kafka topic 可消费到事件

---

### 6. Kafka 事件到达验收（M3.1）

脚本：`scripts/verify_behavior_events.sh`

```bash
export API_BASE=http://localhost:18080
export VIDEO_ID=1
export ACCOUNT=13538167679
export PASSWORD=password
bash scripts/verify_behavior_events.sh
```

通过标准：
- 触发 `play` 与 `like` 后，Kafka `behavior-events` 至少能消费到 1 条消息

---

### 7. 启动 Streaming 实时统计（M3.2）

推荐：使用仓库脚本固定 Spark 3.5.1（更适配 Java 17）

```bash
scripts/run_m3_2_streaming.sh
```

通过标准：
- Streaming 日志显示已连接 Kafka/Redis 并持续运行
- Redis 出现 key：
  - `stats:video:{videoId}`（Hash）
  - `hot:videos`（ZSET）

观测命令（可选）：

```bash
redis-cli -h 127.0.0.1 -p 6379 HGETALL stats:video:1
redis-cli -h 127.0.0.1 -p 6379 ZREVRANGE hot:videos 0 9 WITHSCORES
```

---

### 8. Streaming 端到端验收（触发点赞 -> Redis -> 接口）

脚本：`scripts/acceptance_m3_2_streaming.sh`

```bash
export API_BASE=http://localhost:18080
export VIDEO_ID=1
export ACCOUNT=你的账号
export PASSWORD=你的密码
scripts/acceptance_m3_2_streaming.sh
```

通过标准：
- Redis `stats:video:{videoId}` 的 `like_count` 增加
- Redis `hot:videos` 对应 `videoId` 的 score 增加
- `/api/rank/hot` 返回的计数与 Redis 一致或能体现 Redis 优先

---

### 9. ALS 离线推荐（M4）最小可复现（可选：CSV）

目标：不依赖 MySQL，直接用 CSV 训练并写 Redis，验证写入与读取

```bash
export ALS_SOURCE=csv
export ALS_INPUT_TYPE=ratings
export ALS_CSV_PATH=bigdata/batch/sample-data/ratings.csv
bigdata/batch/bin/run_als.sh
redis-cli -h 127.0.0.1 -p 6379 LRANGE rec:user:1 0 9
```

通过标准：
- Redis 出现 `rec:user:{userId}` 且有元素

---

### 10. ALS 离线推荐端到端验收（JDBC -> Redis -> 推荐接口）

脚本：`scripts/acceptance_m4_als.sh`

前置条件：
- MySQL/Redis 可用
- 后端启用 ALS 开关（以代码实现为准，通常为 `RECO_ALS_ENABLED=true`）

```bash
export API_BASE=http://localhost:18080
export VIDEO_ID=1
export ACCOUNT=13538167679
export PASSWORD=password
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export JDBC_URL='jdbc:mysql://127.0.0.1:3307/shortvideo_recsys?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
export JDBC_USER=app
export JDBC_PASSWORD=apppass
scripts/acceptance_m4_als.sh
```

通过标准：
- Redis `rec:user:{userId}` 可读取到推荐列表
- 若后端开关开启：`/api/recommendations` 返回顺序与 Redis 推荐一致（至少前 N 个一致）
- 若未开启或未命中：接口仍可回退规则推荐并返回非空结果

---

## 常见问题与排查

### 1) 后端接口 404/401
- 确认 `SPRING_PROFILES_ACTIVE=docker` 或 `test`
- 确认请求路径与鉴权（JWT/Bearer；管理端 Basic Auth）

### 2) 行为链路无消息
- 确认 `BEHAVIOR_LOG_PATH` 与 Flume 挂载路径一致（推荐 `deploy/data/behavior/behavior-events.log`）
- 确认 Flume 容器运行并能连接 Kafka

### 3) Streaming 无写入 Redis
- 优先使用 `scripts/run_m3_2_streaming.sh`（固定 Spark 3.5.1）
- 查看 Streaming 日志是否成功连接 Kafka（对外 9093 / 容器内 9092）与 Redis

### 4) ALS 无写入推荐
- 先跑 CSV 最小可复现确认 Spark/Redis 正常
- 再跑 JDBC 模式确认 MySQL 连接串、用户密码与表数据存在

