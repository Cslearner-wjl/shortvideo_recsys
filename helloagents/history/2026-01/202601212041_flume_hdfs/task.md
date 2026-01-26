# 任务清单: Flume HDFS 落地链路与源到汇验收

目录: `helloagents/plan/202601212041_flume_hdfs/`

---

## 1. HDFS 本地部署与配置
- [√] 1.1 在 `deploy/docker-compose.yml` 中新增 HDFS（NameNode/DataNode）服务与卷挂载，验证 why.md#需求-flume--hdfs-落地与断点续传-场景-flume-重启恢复
- [√] 1.2 在 `deploy/.env.example` 中补充 HDFS 相关环境变量示例，验证 why.md#需求-flume--hdfs-落地与断点续传-场景-flume-重启恢复

## 2. Flume Kafka→HDFS 链路
- [√] 2.1 在 `deploy/flume/flume.conf` 中新增 Kafka Source + HDFS Sink + file channel，验证 why.md#需求-flume--hdfs-落地与断点续传-场景-flume-重启恢复
- [√] 2.2 在 `deploy/docker-compose.yml` 中为 Flume 增加持久化目录（channel checkpoint/data），验证 why.md#需求-flume--hdfs-落地与断点续传-场景-flume-重启恢复

## 3. 文档与知识库同步
- [√] 3.1 更新 `bigdata/streaming/README.md` 与 `docs/运行指南.md`，加入 HDFS 落地与验收步骤，验证 why.md#需求-streamingbatch-产物可用-场景-端到端推荐落地
- [√] 3.2 更新 `helloagents/wiki/arch.md`（架构图与说明包含 HDFS），验证 why.md#需求-flume--hdfs-落地与断点续传-场景-flume-重启恢复

## 4. 安全检查
- [√] 4.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 5. 端到端验收与观测
- [√] 5.1 启动 docker-compose 依赖并验证 Kafka topic 生产/消费与 lag，验证 why.md#需求-kafka-可用与分区策略合理-场景-生产消费与-lag-观测
- [√] 5.2 触发行为日志并验证 HDFS 落地与 Flume 重启恢复，验证 why.md#需求-flume--hdfs-落地与断点续传-场景-flume-重启恢复
- [√] 5.3 运行 Streaming 与 ALS（如启用），验证 Redis 写入与推荐接口回退，验证 why.md#需求-streamingbatch-产物可用-场景-端到端推荐落地 与 why.md#需求-推荐接口回退链路-场景-缓存缺失回退
