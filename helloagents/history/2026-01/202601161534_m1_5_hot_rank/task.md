# 任务清单：M1.5 热门榜单（热度计算 + Redis 缓存）

目录：`helloagents/plan/202601161534_m1_5_hot_rank/`

---

## 1. 热度计算与刷新服务

- [√] 1.1 在 `backend` 新增热度配置 `HotRankProperties`（权重、刷新间隔、TopN、开关）。
- [√] 1.2 新增 `HotScoreCalculator`（log1p 加权），支持参数化权重。
- [√] 1.3 新增 `HotRankService.refresh()`：更新 `video_stats.hot_score` 并刷新 Redis ZSET `hot:videos`（仅 TopN）。
- [√] 1.4 为 `test` profile 提供内存榜单缓存实现（不依赖 Redis），并在测试配置中关闭调度。

## 2. API：热门榜单

- [√] 2.1 新增 `GET /api/rank/hot`：分页返回热门视频（基础信息 + 统计字段）。
- [√] 2.2 放行匿名 GET `/api/rank/**`（更新安全配置）。
- [√] 2.3 新增管理端刷新接口 `POST /api/admin/rank/hot/refresh` 便于验收脚本调用。

## 3. 配置与依赖

- [√] 3.1 增加 Redis 依赖（Spring Data Redis），并在 `application.yml` 的 `docker` profile 增加 Redis 连接配置。

## 4. 测试

- [√] 4.1 单元测试：热度公式计算。
- [√] 4.2 集成测试：构造多视频计数 → refresh → `/api/rank/hot` 排序与字段断言。
  > 备注: 为保证 `test` profile 可离线运行，调整 MinIO bucket 初始化仅在 `docker` profile 执行。

## 5. 验收脚本

- [√] 5.1 提供脚本：创建数据 → 调用 refresh → 调用 `/api/rank/hot` 查看排行（可复现）。

## 6. 安全检查

- [√] 6.1 确认无明文密钥提交；Redis/MySQL 等连接信息仅从环境变量/本地配置读取。

## 7. 文档与知识库同步

- [√] 7.1 更新 `helloagents/wiki/api.md`、`helloagents/wiki/modules/backend.md`（新增热门榜单能力）。
- [√] 7.2 更新 `helloagents/CHANGELOG.md` 与 `helloagents/history/index.md`，并按规则迁移方案包至 `helloagents/history/2026-01/`。
