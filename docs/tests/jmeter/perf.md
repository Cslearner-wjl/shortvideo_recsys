# 测试报告 - JMeter 压力测试

## 1. 基本信息

- 测试类型: 压力测试
- 模块: backend API
- 执行日期: 2026-01-22
- 执行人: HelloAGENTS
- 版本: Unreleased

## 2. 测试环境

- 执行工具: Apache JMeter 5.6.x（优先本机执行；也支持 Docker 镜像 justb4/jmeter）
- 后端: `backend/run_backend_18080.sh`（docker profile）
- 目标地址: `http://localhost:18080`（或容器模式下 `http://host.docker.internal:18080`）

## 3. 测试范围

- `POST /api/auth/login`
- `GET /api/videos/page`
- `POST /api/videos/{id}/like`
- `GET /api/videos/{id}/comments`
- `POST /api/videos/{id}/comments`

## 4. 压测配置

- 线程数: 50
- Ramp-up: 10s
- 循环次数: 20
- 总请求量: 2000
- 测试计划: `tests/jmeter/backend_api.jmx`

## 5. 测试结果（关键指标）

- 结论: 通过
- 吞吐量: ~199 req/s
- 平均响应时间: ~5 ms
- 最大响应时间: 150 ms
- 错误率: 0%

## 6. 风险与问题

- 存在早期失败结果 `results_20260122125825.jtl`，验收以 `results_20260122125942.jtl` 为准

## 7. 附件

- JMeter 结果文件: `docs/tests/jmeter/results_20260122125942.jtl`
- HTML 报告目录: `docs/tests/jmeter/report_20260122125942/`

## 8. 压测增强（混合/摸高/浸泡）

- 执行日期: 2026-01-22
- 总报告: `docs/tests/jmeter/stress-enhanced-report.md`
- 阶段一: `docs/tests/jmeter/stage1-mix-report.md`
- 阶段二: `docs/tests/jmeter/stage2-peak-report.md`
- 阶段三: `docs/tests/jmeter/stage3-soak-report.md`

## 9. 稳定性修复与复测记录

- 修复文档: `docs/tests/jmeter/stability-fix-report.md`
