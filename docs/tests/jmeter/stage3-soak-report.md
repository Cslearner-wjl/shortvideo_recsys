# 阶段三：浸泡测试报告（稳定性验证）

## 基本信息
- 执行日期: 2026-01-22
- 目标地址: http://localhost:18080
- 测试脚本: scripts/jmeter_stage3_soak.sh
- 测试计划: tests/jmeter/backend_api_peak_soak.jmx
- 认证方式: 登录获取 Token（账号通过环境变量传入，不落盘）

---

## A. 完整浸泡（30 分钟，推荐作为稳定性验收口径）

### 测试配置
- 并发线程数: 210（摸高 max_ok_threads=300 的 70%）
- Ramp-up: 10s
- 测试时长: 1800s
- 连接超时: 5000ms
- 响应超时: 10000ms
- 结果文件: docs/tests/jmeter/results_stage3_20260122211710.jtl
- HTML 报告: docs/tests/jmeter/report_stage3_20260122211710/
- 摘要文件: docs/tests/jmeter/soak_summary_20260122211710.txt

### 关键结果（业务 HTTP 口径：排除 login/warmup/非 HTTP Sampler）
- 总请求数: 645,645
- 持续时长: 1798.60s
- 吞吐量: 358.97 req/s
- 平均响应: 122.03ms
- P95/P99: 682 / 877 ms
- 错误率: 0%

### 端点维度（业务 HTTP 口径）
- GET /api/videos/page: 517,083 次，错误率 0%，avg 127.3ms，p95 707ms
- POST /api/videos/{id}/like: 64,546 次，错误率 0%，avg 90.0ms，p95 467ms
- GET /api/videos/{id}/comments: 32,059 次，错误率 0%，avg 118.8ms，p95 655ms
- POST /api/videos/{id}/comments: 31,957 次，错误率 0%，avg 104.4ms，p95 496ms

### 稳定期观察（排除前 10 分钟冷启动/缓存预热）
说明：本项目压测包含缓存与多表读写，前几分钟可能出现明显“冷启动抖动”；稳定性结论以稳定期为准。

- GET /api/videos/page: avg 13.2ms, p95 16ms, p99 23ms
- POST /api/videos/{id}/like: avg 18.1ms, p95 23ms, p99 31ms
- GET /api/videos/{id}/comments: avg 11.8ms, p95 14ms, p99 21ms
- POST /api/videos/{id}/comments: avg 29.0ms, p95 37ms, p99 48ms

结论：30 分钟内无错误率抬升，稳定期响应时间无随时间劣化迹象（系统稳定）。

---

## B. 快速浸泡（10 分钟，用于日常回归/冒烟）

### 测试配置
- 测试时长: 600s
- 结果文件: docs/tests/jmeter/results_stage3_20260122223843.jtl
- HTML 报告: docs/tests/jmeter/report_stage3_20260122223843/

### 关键结果（业务 HTTP 口径）
- 总请求数: 142,958
- 持续时长: 599.16s
- 吞吐量: 238.60 req/s
- 平均响应: 810.37ms
- P95/P99: 1465 / 1766 ms
- 错误率: 0%

说明：10 分钟窗口较短，易被“冷启动/缓存预热”放大尾延迟；不建议作为容量/稳定性最终结论，仅用于快速验证“错误率是否归零”。
