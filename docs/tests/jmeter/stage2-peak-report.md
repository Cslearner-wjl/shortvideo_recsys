# 阶段二：摸高测试报告（阶梯加压）

## 基本信息
- 执行日期: 2026-01-22
- 目标地址: http://localhost:18080
- 测试脚本: scripts/jmeter_stage2_peak.sh
- 测试计划: tests/jmeter/backend_api_peak_soak.jmx（登录/预热一次，循环仅业务动作）
- 认证方式: 登录获取 Token（账号通过环境变量传入，不落盘）

## 测试配置
- 起始线程数: 100
- 递增步长: +50/分钟
- 单步持续时间: 60s
- 阈值: p95 > 500ms 或错误率 > 1%
- 连接超时: 5000ms
- 响应超时: 10000ms
- 结果文件: docs/tests/jmeter/peak_results_20260122204652.csv
- 摘要文件: docs/tests/jmeter/peak_summary_20260122204652.txt

## 关键结果（本轮复测）
- max_ok_threads=300
- max_ok_rps=411.67
- stop_reason=p95_over_500（350 线程 p95=512ms）
- 错误率: 各阶梯均为 0%

## 阶梯结果摘要（业务 HTTP 口径）
- 100 线程: samples=29823, error_rate=0.0, avg=30.95ms, p95=109ms, rps=503.84
- 150 线程: samples=30309, error_rate=0.0, avg=65.69ms, p95=255ms, rps=512.39
- 200 线程: samples=25599, error_rate=0.0, avg=30.20ms, p95=84ms, rps=432.07
- 250 线程: samples=25573, error_rate=0.0, avg=42.29ms, p95=185ms, rps=430.80
- 300 线程: samples=24423, error_rate=0.0, avg=52.02ms, p95=295ms, rps=411.67
- 350 线程: samples=23934, error_rate=0.0, avg=79.49ms, p95=512ms, rps=402.69（触发阈值停止）

## 结论
- 系统在 300 并发线程下保持稳定（p95 < 500ms 且错误率 < 1%）；350 线程开始出现明显尾延迟抬升（p95 超阈值）。
