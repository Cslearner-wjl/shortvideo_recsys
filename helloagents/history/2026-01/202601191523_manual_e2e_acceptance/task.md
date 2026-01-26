# 任务清单: 手动验收整套系统（前后端 + 大数据链路）

> 说明：本方案包用于“验收执行指南”，不涉及业务代码改动。

## 任务

- [√] 01-环境准备：确认 Docker/JDK17/Maven/Node 可用（可选 python3/redis-cli）
- [√] 02-启动依赖：compose 启动 mysql/redis/minio/zookeeper/kafka/flume 并确认容器健康
- [√] 03-启动后端：以 `docker` profile 启动到 `18080` 并通过 `/api/health`
- [√] 04-准备验收数据：确保存在至少 1 个 `APPROVED` 视频与可登录用户（可通过前端/接口/脚本/SQL）
- [-] 05-前端用户端：启动 `frontend/user` 并完成最小联调（登录/视频流/互动/热门榜/推荐）
- [-] 06-前端管理端：启动 `frontend/admin` 并完成最小联调（Basic Auth 登录/用户管理/视频管理/看板）
- [√] 07-行为日志落盘：设置 `BEHAVIOR_LOG_PATH=deploy/data/behavior/behavior-events.log` 并确认文件有新增行
- [√] 08-M3.1 验收：执行 `scripts/verify_behavior_events.sh`，确认 Kafka `behavior-events` 可消费到事件
- [√] 09-M3.2 启动：执行 `scripts/run_m3_2_streaming.sh` 启动 Streaming 作业
- [√] 10-M3.2 验收：执行 `scripts/acceptance_m3_2_streaming.sh`，确认 Redis 统计与 `/api/rank/hot` 对齐
- [-] 11-M4（可选-最小复现）：ALS 以 CSV 模式训练并写入 Redis，确认 `rec:user:{userId}` 可读
- [√] 12-M4 验收：执行 `scripts/acceptance_m4_als.sh`，确认 JDBC->Redis 与推荐接口优先读取/回退逻辑
- [-] 13-问题记录：将验收失败点按“步骤/日志/复现命令/期望 vs 实际”记录到外部文档或 issue
- [-] 14-收尾：停止 compose、清理临时数据（可选）

## 验证与安全检查

- [√] 验证：全流程至少跑通一次（不要求一次性无失败，但每个失败应能定位根因）
- [√] 安全：确认未在任何文档中写入明文密码/密钥/令牌；仅使用变量名或示例
