# 技术设计: JMeter 压测加强（混合场景/摸高/浸泡）

## 技术方案
### 核心技术
- JMeter 5.6.2（现有版本）
- JMeter CLI + JTL + Dashboard 报告
- Bash 脚本编排多阶段压测

### 实现要点
- JMX 统一承载混合场景：通过控制器实现 80/10/5/5 权重
- 登录与鉴权：使用 `/api/auth/login` 获取 Token，并通过 Header 统一注入
- 动态视频 ID：通过 `GET /api/videos/page` 抽取 `items[0].id`，用于点赞/评论
- 可配置化参数：线程数/持续时间/目标地址/账号密码通过 `-J` 参数注入
- 摸高测试：脚本每 60 秒递增线程数并分析 JTL 的错误率与响应时间，超过阈值自动停止
- 浸泡测试：基于摸高峰值 70% 并发，持续 30+ 分钟，输出稳定性结论

## 安全与性能
- **安全:** 登录凭据通过运行参数传入，不在脚本中明文固化
- **性能:** JTL 使用分阶段文件命名，避免覆盖；报告输出至 `docs/tests/jmeter/`

## 测试与部署
- **测试:** `scripts/jmeter_stage1_mix.sh`、`scripts/jmeter_stage2_peak.sh`、`scripts/jmeter_stage3_soak.sh`
- **部署:** 无
