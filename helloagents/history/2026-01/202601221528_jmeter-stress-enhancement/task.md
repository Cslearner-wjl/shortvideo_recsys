# 任务清单: JMeter 压测加强（混合场景/摸高/浸泡）

目录: `helloagents/plan/202601221528_jmeter-stress-enhancement/`


## 1. 混合场景脚本
- [√] 1.1 更新 `tests/jmeter/backend_api.jmx`，加入登录取 Token、动态抽取视频 ID，并构建 80/10/5/5 混合控制器，验证 why.md#需求-混合场景构建-场景-读写混合比例

## 2. 阶段执行脚本
- [√] 2.1 新增 `scripts/jmeter_stage1_mix.sh` 执行混合场景基础压测并生成 JTL/报告
- [√] 2.2 新增 `scripts/jmeter_stage2_peak.sh` 实现阶梯加压与阈值停止逻辑，验证 why.md#需求-摸高测试-场景-100-线程起步逐分钟-50
- [√] 2.3 新增 `scripts/jmeter_stage3_soak.sh` 基于阶段二结果执行 30+ 分钟浸泡，验证 why.md#需求-浸泡测试-场景-峰值-70-并发运行-30-分钟

## 3. 安全检查
- [√] 3.1 执行安全检查（凭据不落盘、压测阈值与停止策略）

## 4. 文档更新
- [√] 4.1 新增 `docs/tests/jmeter/stress-enhanced-report.md` 记录三阶段结果与结论
- [√] 4.2 更新 `docs/tests/jmeter/perf.md` 追加本次压测入口与报告链接
- [√] 4.3 更新 `helloagents/wiki/modules/docs.md` 与 `helloagents/wiki/modules/scripts.md`，同步压测流程入口
- [√] 4.4 更新 `helloagents/CHANGELOG.md`

## 5. 测试
- [√] 5.1 执行阶段一压测脚本并生成报告
- [√] 5.2 执行阶段二摸高脚本并生成报告
  > 备注: 起始 100 线程即触发错误率阈值；health 502，需恢复服务后重测。
- [√] 5.3 执行阶段三浸泡脚本并生成报告
  > 备注: Docker daemon 报错 500，改用本地 JMeter 5.6.3 完成 30 分钟浸泡；服务超时严重，需恢复后重测。
