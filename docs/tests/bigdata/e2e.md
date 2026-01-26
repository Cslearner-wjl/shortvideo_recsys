# 测试报告 - 大数据链路端到端验证

## 1. 基本信息

- 测试类型: 端到端（E2E）
- 模块: bigdata
- 执行日期: 2026-01-22
- 执行人: HelloAGENTS
- 版本: Unreleased

## 2. 测试环境

- Docker Compose: Kafka / Zookeeper / Redis / HDFS / Flume
- 后端: docker profile（18080）
- Spark Streaming: `scripts/run_m3_2_streaming.sh`

## 3. 测试范围

- 行为事件写入 → Kafka 消费
- Streaming 写入 Redis
- 热门榜接口读取 Redis 统计

## 4. 执行步骤

- `docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d`
- `bash scripts/run_m3_2_streaming.sh`
- `ACCOUNT=testuser PASSWORD=Test123456 VIDEO_ID=1 bash scripts/acceptance_m3_2_streaming.sh`
- `ACCOUNT=testuser PASSWORD=Test123456 bash scripts/verify_behavior_events.sh`

## 5. 测试结果

- 结论: 通过
- 关键验证:
  - Redis `stats:video:{id}` / `hot:videos` 可读
  - Kafka `behavior-events` 消费到消息
  - `/api/rank/hot` 返回数据正常

## 6. 风险与问题

- 无

## 7. 附件

- 验收脚本: `scripts/acceptance_m3_2_streaming.sh` / `scripts/verify_behavior_events.sh`
