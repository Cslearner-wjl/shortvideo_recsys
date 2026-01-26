# 变更提案: M3.1 大数据链路（应用日志 -> Flume -> Kafka）

## 需求背景
当前用户行为仅落库到 `user_actions`，缺少可被实时/离线链路消费的日志流，难以支撑后续大数据分析与推荐特征提取。

## 产品分析

### 目标用户与场景
- **用户群体:** 数据工程/算法与运维
- **使用场景:** 行为日志实时采集、回溯分析、质量排查
- **核心痛点:** 行为数据无法可靠流入 Kafka，缺少统一事件格式

### 价值主张与成功指标
- **价值主张:** 建立最小可用的行为日志链路，为后续离线/实时分析打底
- **成功指标:** 行为事件写入日志并被 Flume 采集；Kafka 能看到 JSON 事件

### 人文关怀
仅采集必要字段，避免记录敏感信息，减少隐私风险。

## 变更内容
1. 后端写入 `user_actions` 时同步追加 JSON 日志（路径可配置）
2. Flume 配置：tail 日志并写入 Kafka topic（`behavior-events`）
3. docker-compose 增加 Flume（如环境允许）或提供本地启动说明
4. 端到端验收脚本：触发点赞/播放并在 Kafka 中看到消息

## 影响范围
- **模块:** backend、deploy、scripts、helloagents/wiki
- **文件:** 后端行为服务、Flume 配置、docker-compose 与验收脚本
- **API:** 无新增
- **数据:** 新增日志流（JSON Lines），不涉及 Redis 写入

## 核心场景

### 需求: 行为日志落地
**模块:** backend
在 `user_actions` 写入成功后，同步追加一行 JSON 事件到日志文件。

#### 场景: 点赞与播放
用户触发点赞/播放后，日志文件新增一条行为事件，包含用户、视频、动作与时间信息。

### 需求: Flume 采集到 Kafka
**模块:** deploy
Flume 读取日志文件并写入 Kafka topic `behavior-events`。

#### 场景: 端到端验收
Kafka console-consumer 能看到新增 JSON 事件。

## 风险评估
- **风险:** 日志路径不可写、Flume 无法访问宿主机路径
- **缓解:** 路径可配置并预创建目录；提供 docker 与本地两种启动说明
- **约束:** 不引入 Spark；不写入 Redis
