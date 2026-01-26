# 阶段一：混合场景压测报告（读/点赞/评论/发评 = 80/10/5/5）

## 基本信息
- 执行日期: 2026-01-22
- 目标地址: http://localhost:18080
- 测试脚本: scripts/jmeter_stage1_mix.sh
- 测试计划: tests/jmeter/backend_api.jmx
- 认证方式: 登录获取 Token（账号通过环境变量传入，不落盘）

## 测试配置
- 线程数: 100
- Ramp-up: 10s
- 循环次数: 20
- 连接超时: 5000ms
- 响应超时: 10000ms
- 结果文件: docs/tests/jmeter/results_stage1_20260122204612.jtl
- HTML 报告: docs/tests/jmeter/report_stage1_20260122204612/

## 关键结果（本轮复测）
- 总请求数（全部 Sampler）: 4200
- 持续时长: 10.14s
- 平均响应: 10.03ms
- P50/P90/P95/P99: 10 / 22 / 30 / 78 ms
- 错误率: 0%

## 端点维度（HTTP 请求）
- GET /api/videos/page: 1628 次，错误率 0%，avg 14.43ms，p95 25ms
- POST /api/videos/{id}/like: 185 次，错误率 0%，avg 23.67ms，p95 38ms
- GET /api/videos/{id}/comments: 99 次，错误率 0%，avg 12.44ms，p95 24ms
- POST /api/videos/{id}/comments: 88 次，错误率 0%，avg 31.39ms，p95 43ms
- POST /api/auth/login: 100 次，错误率 0%，avg 78.20ms，p95 99ms

## 结论
- 混合场景下读写均稳定，未再出现大量 500（评论写入链路稳定性已恢复）。
