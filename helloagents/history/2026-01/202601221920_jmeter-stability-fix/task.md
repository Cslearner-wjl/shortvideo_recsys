# 任务清单: JMeter 压测稳定性修复（死锁重试 + 压测模型修正）

目录: `helloagents/plan/202601221920_jmeter-stability-fix/`


## 1. 后端稳定性修复（死锁重试）
- [√] 1.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/video/VideoInteractionService.java` 中为写链路引入事务级重试（有限次 + 退避抖动），覆盖发评/点赞等操作，验证 why.md#需求-混合场景下写请求稳定性-场景-高并发发评同一视频与多视频混合
  > 备注: 同步修复 `ensureStatsRow` 的“先插入再更新”高并发死锁根因（避免 S->X 锁升级死锁）。
- [√] 1.2 （可选）优化 `backend/src/main/java/com/shortvideo/recsys/backend/bigdata/BehaviorEventLogger.java` 在压测下的日志/IO开销，避免对接口吞吐造成明显影响

## 2. 压测计划修正（peak/soak）
- [√] 2.1 新增 `tests/jmeter/backend_api_peak_soak.jmx`：将登录与视频 ID 抽样置于循环之外，并在循环内仅执行 80/10/5/5 业务动作，验证 why.md#需求-peaksoak-负载模型与真实用户一致-场景-仅业务动作进入循环
- [√] 2.2 更新 `scripts/jmeter_stage2_peak.sh` 与 `scripts/jmeter_stage3_soak.sh` 使用新的 peak/soak JMX，保持阶段一仍使用 `tests/jmeter/backend_api.jmx`

## 3. 安全检查
- [√] 3.1 执行安全检查（凭据不落盘、避免在报告中输出明文密码、重试不导致数据不一致）

## 4. 文档更新
- [√] 4.1 新增修复文档 `docs/tests/jmeter/stability-fix-report.md`：记录问题、定位过程、修复过程与最终压测结果
- [√] 4.2 更新阶段报告与汇总报告（必要时新增新一轮报告文件），确保 `docs/tests/jmeter/perf.md` 有可追溯入口
- [√] 4.3 同步知识库（`helloagents/wiki/modules/docs.md`、`helloagents/wiki/modules/scripts.md`）与 `helloagents/CHANGELOG.md`

## 5. 复测（直到稳定）
- [√] 5.1 重新执行阶段一混合场景（100线程*20循环）并生成 JTL/HTML 报告
- [√] 5.2 重新执行阶段二摸高（阶梯加压）并确定最大稳定并发与RPS
- [√] 5.3 重新执行阶段三浸泡（峰值70%并发，30+分钟）并产出稳定性结论
  > 备注: 额外补充 10 分钟快速浸泡作为冒烟回归；后续按用户要求不再继续运行浸泡测试。
