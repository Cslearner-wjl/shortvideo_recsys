# 变更提案: JMeter 压测稳定性修复（死锁重试 + 压测模型修正）

## 需求背景
近期 JMeter 压测报告显示系统在混合写场景下出现较高错误率与抖动，且摸高/浸泡阶段的负载模型存在偏差，导致结果失真，难以得到真实容量结论。

## 变更内容
1. 修复后端在高并发发评场景下的数据库死锁导致的 500（增加事务级重试与退避）。
2. 修正 peak/soak 压测计划的“登录/预热重复执行”问题，确保负载符合真实用户模型（登录/抽样一次，循环仅覆盖业务动作）。
3. 重新执行三阶段压测并沉淀修复过程文档与最终结果。

## 影响范围
- **模块:**
  - backend（互动写链路稳定性）
  - tests/jmeter、scripts（压测计划与执行脚本）
  - docs/tests/jmeter（报告与修复文档）
- **文件:**
  - `backend/src/main/java/com/shortvideo/recsys/backend/video/VideoInteractionService.java`
  - `backend/src/main/java/com/shortvideo/recsys/backend/bigdata/BehaviorEventLogger.java`（若涉及压测性能瓶颈）
  - `tests/jmeter/*.jmx`
  - `scripts/jmeter_stage2_peak.sh`
  - `scripts/jmeter_stage3_soak.sh`

## 核心场景

### 需求: 混合场景下写请求稳定性
**模块:** backend
在大量并发 `POST /api/videos/{id}/comments` 时，不应因 MySQL 死锁直接返回 500。

#### 场景: 高并发发评（同一视频与多视频混合）
- 预期结果: 业务接口错误率维持在可接受阈值内（默认以 <1% 为目标），且不出现持续性 500 风暴
- 预期结果: 后端日志不再出现大量 `DeadlockLoserDataAccessException` 未处理异常

### 需求: peak/soak 负载模型与真实用户一致
**模块:** tests/jmeter
摸高与浸泡阶段应避免反复登录/反复预热导致的非业务压力放大。

#### 场景: 仅业务动作进入循环
- 预期结果: 单次压测中，每个虚拟用户只在启动时登录与抽取视频 ID；循环阶段仅包含“刷/赞/看评/发评”

## 风险评估
- **风险:** 事务重试可能导致单请求尾延迟上升
- **缓解:** 采用有限次重试（默认 3 次）+ 指数退避 + 抖动，超过上限仍返回错误并记录可观测日志

