# docs

## 目的

存放需求、流程与约束等外部文档，为代码实现与知识库更新提供输入材料。

## 模块概览

- **职责:** SRS/流程/技术约束等外部文档沉淀
- **状态:** 规划中
- **最后更新:** 2026-01-22

---

## 规范

- `docs/` 作为输入材料来源；当与代码不一致时，以代码与运行时行为为准，并同步更新 `helloagents/` 知识库。
- 测试报告统一落地 `docs/tests/`，按模块与测试类型分层存档。
- 文档中禁止硬编码第三方 API Key/Token；统一使用环境变量或 `.env` 注入（仓库仅提交 `.env.example`）。
- 近期汇总文档: `docs/test/backend/surefire-test-summary.md`（如需统一到 `docs/tests/` 请迁移）。
- 近期专项报告: `docs/test/backend/permissions-validation-report.md`、`docs/test/backend/recommendation-rank-report.md`、`docs/test/backend/video-interaction-report.md`
- JMeter 压测增强报告: `docs/tests/jmeter/stage1-mix-report.md`、`docs/tests/jmeter/stage2-peak-report.md`、`docs/tests/jmeter/stage3-soak-report.md`、`docs/tests/jmeter/stress-enhanced-report.md`
- JMeter 稳定性修复报告: `docs/tests/jmeter/stability-fix-report.md`



### 需求: 启动文档迁移与入口整理
**模块:** docs
将启动后端、前端、依赖等说明集中到 docs，README 保留入口与链接。

#### 场景: 启动指南集中
打开 docs 即可定位启动步骤与脚本说明。
- 预期结果: docs 具备清晰入口与章节标识

### 需求: 系统级详细设计文档
**模块:** docs
沉淀系统级详细设计文档，统一架构、数据链路、测试与运维基线。

#### 场景: 设计评审与验收
评审人员可通过单一文档理解全链路并形成测试与上线依据。
- 预期结果: 文档覆盖架构、数据流、API、数据模型与压测要求
