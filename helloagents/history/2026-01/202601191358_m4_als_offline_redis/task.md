# 任务清单: M4 Spark ALS 离线协同过滤推荐 → Redis

目录: `helloagents/plan/202601191358_m4_als_offline_redis/`

---

## 1. bigdata/batch（离线 ALS 作业）
- [√] 1.1 新增 Maven 模块 `bigdata/batch`（pom 依赖 Spark SQL + MLlib + Redis 客户端），补齐 `README.md` 与运行脚本
- [√] 1.2 实现训练数据读取（JDBC 读取 MySQL `user_actions`）并支持最小可复现 CSV 输入，验证 why.md#需求-als-离线训练与写入-redis-场景-训练作业本地运行（最小可复现）
- [√] 1.3 实现评分映射与聚合（按 user_id/video_id 聚合），输出 DataFrame: `user, item, rating`
- [√] 1.4 实现 ALS（implicit feedback）训练与 TopN 生成（可配置参数），并支持简单 hit@K（可选）
- [√] 1.5 实现 Redis 写入：`rec:user:{userId}`（List），包含 `DEL + RPUSH + EXPIRE` 且支持 pipeline

## 2. backend（优先 ALS 推荐读取）
- [√] 2.1 新增/扩展推荐配置：`app.recommendations.als.*`（enabled/topn/redis-prefix/ttl 等）
- [√] 2.2 在推荐服务中实现“优先 ALS → fallback 规则推荐”的读取逻辑，验证 why.md#需求-后端优先读取-als-推荐-场景-开关开启且命中-als
- [√] 2.3 增加最小集成测试（或在现有测试中扩展）：Redis 命中时返回 ALS 顺序；未命中时回退规则

## 3. 文档与运行说明
- [√] 3.1 更新 `helloagents/wiki/modules/bigdata.md`：新增 batch 作业说明、参数与运行方式
- [√] 3.2 更新 `helloagents/wiki/api.md`：补充推荐接口读取策略（ALS 优先 + fallback）
- [√] 3.3 增补一套最小可复现实验数据（CSV/SQL）与端到端验收流程说明

## 4. 安全检查
- [√] 4.1 执行安全检查（按G9：不写明文密钥；JDBC/Redis 通过环境变量注入；避免生产环境误操作）

## 5. 测试
- [√] 5.1 本地端到端验证：触发点赞→DB `user_actions` 增量→运行 batch→Redis `rec:user:{userId}` 变化→接口响应变化，验证 why.md#需求-后端优先读取-als-推荐-场景-开关开启且命中-als
  > 备注: 已在本地 docker 环境执行验收脚本验证通过（MySQL/Redis/后端联动）。
