# 技术设计: M3.1 大数据链路（应用日志 -> Flume -> Kafka）

## 技术方案
### 核心技术
- 后端：Spring Boot + Jackson（JSON 输出）
- 采集：Apache Flume（TAILDIR Source）
- 消息：Kafka（topic: `behavior-events`）

### 事件 Schema

**主题命名:** `behavior-events`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| eventId | string | 是 | 事件唯一标识（UUID） |
| eventType | string | 是 | 行为类型（PLAY/LIKE/UNLIKE/FAVORITE/UNFAVORITE/COMMENT） |
| userId | number | 是 | 用户 ID |
| videoId | number | 是 | 视频 ID |
| actionTime | string | 是 | 行为时间（ISO 8601/本地时区） |
| actionTs | number | 是 | 行为时间戳（毫秒） |
| durationMs | number | 否 | 播放时长（仅 PLAY） |
| isCompleted | boolean | 否 | 播放是否完成（仅 PLAY） |
| source | string | 是 | 事件来源（backend） |

### 日志文件格式
- **格式:** JSON Lines（每行一条 JSON）
- **编码:** UTF-8
- **文件路径:** `app.behavior-log.path` 可配置，默认 `./logs/behavior-events.log`
- **失败策略:** 写日志失败不影响主流程，但会输出告警日志

### 后端实现
- 新增 `BehaviorEventLogger` 组件：
  - 读取配置 `app.behavior-log.enabled/path`
  - 负责事件组装与文件追加写入（`CREATE + APPEND`）
  - 使用 `ObjectMapper` 输出 JSON
  - 采用轻量锁或单线程写入，避免多线程行内拼接
- 在 `VideoInteractionService` 中：
  - `play/like/unlike/favorite/unfavorite/comment` 写库后调用记录方法
  - 事件数据与 `user_actions` 对齐

### Flume 配置（deploy/flume/flume.conf）
- Source: `TAILDIR`，监听日志文件
- Channel: `memory`
- Sink: `kafka`
- 关键参数示例：
  - `agent.sources.s1.type = TAILDIR`
  - `agent.sources.s1.filegroups = f1`
  - `agent.sources.s1.filegroups.f1 = /data/behavior/behavior-events.log`
  - `agent.sources.s1.positionFile = /data/behavior/taildir_position.json`
  - `agent.sinks.k1.type = kafka`
  - `agent.sinks.k1.kafka.topic = behavior-events`
  - `agent.sinks.k1.kafka.bootstrap.servers = kafka:9092`

### docker-compose 改动
- 新增 `flume` 服务：
  - 读 `deploy/flume/flume.conf`
  - 挂载行为日志目录 `/data/behavior`
  - 依赖 `kafka`
- 若无法引入容器：提供本地启动命令
  - `flume-ng agent -n agent -c conf -f deploy/flume/flume.conf -Dflume.root.logger=INFO,console`

## 验收步骤（脚本化）

1. 启动基础依赖：`docker-compose up -d zookeeper kafka`
2. 启动后端（docker/test profile）并设置日志路径：
   - `APP_BEHAVIOR_LOG_PATH=/path/to/behavior-events.log`
3. 触发一次点赞/播放：
   - 登录获取 token（或使用已存在用户）
   - `POST /api/videos/{id}/play` 与 `POST /api/videos/{id}/like`
4. Kafka 消费：
   - `kafka-console-consumer --bootstrap-server kafka:9092 --topic behavior-events --from-beginning --max-messages 1`
5. 看到 JSON 事件并包含 `eventType/userId/videoId/actionTime` 字段

## 安全与性能
- 不记录密码/邮箱等敏感信息
- 日志写入失败不影响主业务
- 保持 JSON 结构稳定，便于下游解析

## 测试与部署
- 添加脚本 `scripts/verify_behavior_events.sh`
- 文档说明 docker 与本地 Flume 两种启动方式
