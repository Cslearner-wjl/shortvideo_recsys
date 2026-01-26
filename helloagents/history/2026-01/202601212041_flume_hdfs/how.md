# 技术设计: Flume HDFS 落地链路与源到汇验收

## 技术方案
### 核心技术
- Docker Compose + Hadoop HDFS（NameNode/DataNode）
- Flume Kafka Source + HDFS Sink
- Spark Structured Streaming / Spark MLlib ALS
- Redis / MySQL

### 实现要点
- 在 `deploy/docker-compose.yml` 增加 HDFS 服务（NameNode/DataNode）与持久化卷。
- Flume 配置新增 Kafka Source 与 HDFS Sink，采用独立 channel，避免与日志→Kafka 采集互相影响。
- HDFS Sink 配置 roll 与 inUse 文件策略，配合 Kafka consumer group 降低重启重复写风险。
- 补充端到端验收说明与观测步骤（topic、lag、HDFS 文件、Redis key、接口回退）。

## 架构设计
```mermaid
flowchart TD
  L[行为日志文件] --> F1[Flume(Taildir)]
  F1 --> K[Kafka]
  K --> F2[Flume(Kafka Source)]
  F2 --> H[HDFS]
  K --> SS[Spark Streaming]
  SS --> R[Redis]
  K --> ALS[Spark ALS Batch]
  ALS --> R
```

## 架构决策 ADR
### ADR-004: 本地 docker-compose 引入 HDFS 并通过 Flume 落地
**上下文:** 现有链路缺少 HDFS 落地，无法满足“源到汇”验收与日志存档需求。  
**决策:** 在本地 compose 中新增 NameNode/DataNode，并由 Flume Kafka Source 写入 HDFS。  
**理由:** 与现有 Flume/Kafka 体系兼容，改动成本低，便于本地验收。  
**替代方案:** Kafka Connect HDFS Sink → 拒绝原因: 引入新组件与配置成本高。  
**影响:** 增加容器资源占用，需管理 HDFS 卷与权限。

## 安全与性能
- **安全:** 不写入敏感密钥；HDFS 仅用于本地环境；避免将 PII 明文外泄到公网。
- **性能:** Kafka Source 使用 consumer group；HDFS roll 参数避免小文件过多；必要时调大 channel 容量。

## 测试与部署
- **部署:** `docker compose` 启动 zookeeper/kafka/flume/hdfs/redis/mysql（按需）。
- **测试:**
  - Kafka topic 生产/消费检查
  - Flume 重启后 HDFS 文件增长检查
  - Streaming 写 Redis 统计与热门榜
  - ALS 写 Redis 推荐列表
  - 推荐接口 Redis 优先与回退路径验证
