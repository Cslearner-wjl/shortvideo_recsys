# Changelog

本文档记录项目所有重要变更。  
格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增
- M0：新增 `deploy/docker-compose.yml`（MySQL/Redis/MinIO/Kafka）与最小可运行的 `backend/`、`frontend/` 工程骨架。
- 数据库：集成 Flyway，并落地核心表迁移脚本与默认管理员种子数据。
- 批量导入：新增 `scripts/batch_import_videos.sh`（CSV/JSON 批量导入 + 自动审核）与 `uploaduser` 预置迁移。
- M1.2：用户注册登录 + JWT 鉴权 + 邮箱验证码最小实现（写表 + 控制台打印）。
- M1.3：视频管理（上传/审核/热门/删除）并接入 MinIO；用户端仅展示审核通过视频。
- M1.4：用户互动与行为采集（play/like/comment/favorite），落库 `user_actions` 并原子更新 `video_stats`；补充登录→点赞→计数变化集成测试。
- M1.5：热门榜单（热度计算 + Redis 缓存）：定时刷新 `video_stats.hot_score` 与 Redis ZSET `hot:videos`，新增 `GET /api/rank/hot` 与 `POST /api/admin/rank/hot/refresh`。
- M1.6：规则推荐（阶段1）：新增 `GET /api/recommendations`，新用户走热门+随机；老用户基于 `user_actions` 计算 tags 偏好并混入热门与随机，支持最近行为去重与滚动参数 `cursor`。
- 管理后台接口：用户管理与看板统计
- M2.2：用户端基础页面与联调（登录/注册/视频流/互动/热门榜）
- M2.3：管理端前端（登录/用户管理/视频管理/数据看板）
- M3.1：行为日志 -> Flume -> Kafka 链路（JSON Lines）
- M3.1+：Flume Kafka Source -> HDFS 行为日志落地（本地 NameNode/DataNode）
- Flume HDFS Sink 通过 `deploy/flume/flume-env.sh` 引入 Spark Hadoop 客户端 jar
- M3.2：Spark Streaming 实时统计（Kafka `behavior-events` -> Redis stats/hash + 热门 ZSET）
- M4：Spark MLlib ALS 离线推荐（MySQL `user_actions`/CSV -> Redis `rec:user:{userId}`），后端推荐接口可按开关优先读取 ALS 结果
- 评论列表 API（分页/时间排序）与评论点赞接口，新增评论点赞数据表与计数字段
- 用户端推荐/榜单新增筛选、排序、封面与统计展示、无限滚动、空态提示
- 管理端登录/用户管理/视频管理/看板统一黑灰极简视觉
- 用户资料完善：新增头像/简介字段与个人资料更新接口、页面
- 用户端视频详情页与评论列表组件（支持分页与评论点赞）
- 播放上报补充时长与完播标记（前端触发）
- 管理端管理员账号管理 CRUD 接口与页面能力补齐
- 管理端看板新增视频发布量统计接口与趋势图表
- 前端测试体系：用户端/管理端引入 Vitest + Playwright，并补齐基础单测与 E2E 用例
- 压测方案：新增 JMeter 压测计划 `tests/jmeter/backend_api.jmx` 与执行报告输出
- 压测脚本：新增混合/摸高/浸泡脚本与定时版测试计划 `tests/jmeter/backend_api_soak.jmx`
- 测试文档：新增 `docs/tests/` 标准测试报告（后端/前端/大数据/压测）
- backend/test：补齐权限/参数校验、推荐/热榜边界、互动异常与幂等集成测试，并产出专项测试报告

### 变更
- 知识库：执行 `~init` 后同步修订 `project.md` 与 `wiki/*`，修复文档过时描述与 Markdown 格式问题。
- 用户端推荐/榜单改为单视频模式，支持上下键切换与自动播放
- 用户端与管理端恢复浅色主题，页面文案统一中文

### 修复
- 用户端互动失败提示升级为弹窗并支持手动重试
- bigdata/streaming：提供项目内固定 Spark 3.5.1 的运行脚本，规避 Spark 3.1.x + Java 17 兼容性问题。
- backend/recommendations：ALS Redis 缓存命中但数据无效/不可用时，自动回退规则推荐，避免返回空列表。
- backend/test：H2 schema 补齐用户头像/简介字段，修复测试查询失败
- backend/admin：视频发布量统计 DTO 改为可映射对象，修复看板测试 500
- 用户端评论数与评论列表不一致问题，评论面板加载后同步 total；点赞/收藏支持切换并补充状态字段
- 用户端评论输入/发送恢复，热门页互动按钮可用，推荐/热门详情按钮上移
- 用户端写操作统一登录态校验与 401 处理，评论/点赞/收藏对接持久化
- backend/test：管理员集成测试密码对齐与 MinIO 集成测试鉴权稳定化
- 压测稳定性：修复高并发评论写入导致的 `video_stats` 死锁与 500；修正 JMeter peak/soak 负载模型（登录/预热一次 + 随机 videoId 抽样）并补齐三阶段复测报告

### 文档
- 补齐知识库模块文档：`frontend`、`scripts`
- 同步 API/架构/数据文档：补充 profile 约定与行为日志采集链路说明
- 补充 HDFS 落地链路与运行指南（docker-compose + 验收要点）
- 统一补充脚本用途与用法注释，便于检索与执行
- 文档迁移至 docs 并新增运行指南与脚本说明
- 生成视频目录迁移至 scripts/生成视频
- 新增系统级详细设计文档（docs/plans）
- 补齐 JMeter 压测增强报告（混合/摸高/浸泡）

## [0.1.0] - 2026-01-15

### 新增

- 初始化 `helloagents/` 知识库骨架（overview/arch/api/data/modules/history）。
