# 任务清单：M1.6 规则推荐（阶段1）

目录：`helloagents/plan/202601180946_m1_6_rule_reco_stage1/`

---

## 1. 推荐配置与画像计算

- [√] 1.1 新增推荐配置 `RecommendationProperties`（候选池大小、TopK、最近行为 N、比例、随机种子等）。
- [√] 1.2 实现 tags 解析工具（容错 JSON array / 逗号分隔）。
- [√] 1.3 实现用户画像计算：从 `user_actions` 计算 tag 偏好 TopK。

## 2. 推荐候选与混排

- [√] 2.1 新用户：热门 TopN + 随机补齐。
- [√] 2.2 老用户：标签优先 + 混入热门与随机（比例可配置）。
- [√] 2.3 去重：按最近 N 条 `user_actions` 过滤已看过/强互动视频。

## 3. API：推荐接口

- [√] 3.1 新增 `GET /api/recommendations`（支持 `page/pageSize` 与 `cursor`）。
- [√] 3.2 返回结构包含 `nextCursor` 与统计字段。

## 4. 测试与 Demo

- [√] 4.1 单元测试：标签解析与画像计算。
- [√] 4.2 集成测试：插入行为 → `/api/recommendations` 输出变化；验证去重与偏好命中。
- [√] 4.3 Demo 脚本：创建数据/行为 → 调用推荐接口并展示变化。

## 5. 安全检查

- [√] 5.1 确认无明文密钥提交；脚本仅使用本地 docker 环境变量。

## 6. 知识库同步与迁移

- [√] 6.1 更新 `helloagents/wiki/api.md`、`helloagents/wiki/modules/backend.md`（新增推荐接口与规则说明）。
- [√] 6.2 更新 `helloagents/CHANGELOG.md` 与 `helloagents/history/index.md`，并迁移方案包至 `helloagents/history/2026-01/`。
