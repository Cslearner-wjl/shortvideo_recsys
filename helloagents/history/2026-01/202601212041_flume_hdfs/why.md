# 变更提案: Flume HDFS 落地链路与源到汇验收

## 需求背景
当前大数据链路已具备行为日志落本地文件、Flume 采集入 Kafka、Streaming 写 Redis、ALS 离线写 Redis 的能力，但 HDFS 落地链路尚未实现，导致“从源到汇”验收不完整，无法覆盖日志存档与离线回溯场景。

## 变更内容
1. 在本地 docker-compose 中补齐 HDFS（NameNode/DataNode）服务。
2. 在 Flume 中新增 Kafka Source → HDFS Sink 落地链路，保留现有日志→Kafka 流程。
3. 补充验收与观测流程，覆盖源/Kafka/Flume/HDFS/Streaming/Batch/Redis 及推荐回退链路。

## 影响范围
- **模块:** deploy, bigdata, helloagents/wiki
- **文件:** deploy/docker-compose.yml, deploy/flume/flume.conf, bigdata/*, helloagents/wiki/*
- **API:** 无
- **数据:** HDFS 路径新增（行为日志落地）

## 核心场景

### 需求: 行为日志字段完整性与异常过滤
**模块:** backend/bigdata
行为日志为 JSON Lines，字段齐全且对异常行为（空 userId/非法 videoId）可被过滤或标记。

#### 场景: 产生日志并校验字段
后端触发播放/点赞等行为
- 日志包含 userId、videoId、eventType、ts(actionTs/actionTime) 等字段
- 空 userId / 非法 videoId 不进入后续链路

### 需求: Kafka 可用与分区策略合理
**模块:** deploy/kafka
topic 存在且可生产/消费，分区策略可按 userId 或 videoId。

#### 场景: 生产/消费与 lag 观测
通过 console producer/consumer 验证
- topic 可用且消息稳定
- 高并发下无明显积压

### 需求: Flume → HDFS 落地与断点续传
**模块:** deploy/flume
Flume 从 Kafka 消费并落 HDFS。

#### 场景: Flume 重启恢复
重启 Flume
- 不重复写入大量旧数据
- 不丢数据（至少一次语义）

### 需求: Streaming/Batch 产物可用
**模块:** bigdata/streaming, bigdata/batch
Streaming 统计更新延迟可接受；ALS 离线结果可读。

#### 场景: 端到端推荐落地
触发行为 → Streaming 更新 Redis
- 实时统计与热门榜可读
- ALS 产出可读

### 需求: 推荐接口回退链路
**模块:** backend/recommendation
推荐接口优先读 Redis，Redis 空则回退规则/热门/离线结果。

#### 场景: 缓存缺失回退
清空/过期 Redis
- 接口可回退返回
- 延迟可接受

## 风险评估
- **风险:** Flume Kafka Source 重启可能导致重复写或漏写（偏移提交与通道持久化策略不当）。
- **缓解:** 使用持久化 channel、合理 offset reset 策略、HDFS roll 配置并进行重启演练。
