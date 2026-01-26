# scripts

## 目的
集中管理用于本地验证、演示与验收的脚本，便于快速复现推荐效果与进行功能验收。

## 模块概述
- **职责:** 提供演示与验收脚本（PowerShell/bash）以驱动后端接口、校验输出
- **状态:** 🟡开发中
\12026-01-21

## 规范

### 需求: 推荐演示脚本
**模块:** scripts
提供可重复执行的推荐演示/验收脚本，输入参数清晰、输出可读、失败原因可定位。

#### 场景: 演示推荐结果
前置条件: 后端服务已启动且可访问
- 通过脚本请求推荐接口并输出结果摘要
- 脚本失败时输出明确错误信息与排查建议

### 需求: 批量导入视频脚本
**模块:** scripts
提供 bash 脚本批量导入真实视频素材，用于用户端播放验证与推荐链路演示。

#### 场景: CSV/JSON 批量上传并自动审核
前置条件: 后端 docker/test profile 已启动；管理端 Basic Auth 可用；uploaduser 已预置（默认口令 password，仅限测试/演示）。
- 支持 CSV/JSON 输入（title/tags 为空时从文件名解析）
- 通过 `/api/admin/videos` 上传并落 MinIO
- 可选自动审核为 `APPROVED`，使用户端可播放
- 输出成功/失败日志便于定位问题
- 依赖工具: `curl`、`jq`
- 管理端认证: 使用 Basic Auth，请根据 `admin_users` 或 `app.admin.bootstrap.*` 配置填写账号密码
- 脚本路径: `scripts/batch_import_videos.sh`

### 需求: M3.2 实时统计验收脚本
**模块:** scripts
提供端到端验收脚本：触发行为事件 → Streaming 写入 Redis → 后端接口返回变化。

#### 场景: 触发点赞并校验 Redis 与接口
前置条件: 后端（docker profile）+ Kafka/Redis/Flume + Streaming 作业已启动
- 调用点赞接口触发 `LIKE` 行为
- 校验 `stats:video:{id}` 与 `hot:videos` 的增量变化
- 请求 `/api/rank/hot` 验证接口优先读取 Redis

### 需求: M4 ALS 离线推荐验收脚本
**模块:** scripts
提供端到端验收脚本：触发点赞落库 → 运行 ALS batch 写 Redis → 推荐接口优先读取 ALS（开关开启）。

#### 场景: 触发点赞并校验 Redis 与推荐接口
前置条件: 后端（docker profile）+ MySQL/Redis 已启动；后端启用 `RECO_ALS_ENABLED=true`
- 调用点赞接口触发 `LIKE` 行为并写入 `user_actions`
- 运行 `scripts/run_m4_als_batch.sh` 写入 `rec:user:{userId}`
- 请求 `/api/recommendations` 验证接口返回顺序与 Redis 推荐一致（未命中回退规则推荐）

### 需求: JMeter 压测脚本
**模块:** scripts
提供混合场景、摸高测试与浸泡测试的自动化脚本，输出报告与 JTL 结果文件。

#### 场景: 压测执行与结果归档
前置条件: 后端服务可访问；登录账号通过环境变量传入
- 混合场景: `scripts/jmeter_stage1_mix.sh`
- 摸高测试: `scripts/jmeter_stage2_peak.sh`
- 浸泡测试: `scripts/jmeter_stage3_soak.sh`
- 结果目录: `docs/tests/jmeter/`
说明:
- 阶段一使用 `tests/jmeter/backend_api.jmx`（混合场景）。
- 阶段二/三使用 `tests/jmeter/backend_api_peak_soak.jmx`（登录/预热一次，循环仅业务动作）。
- 浸泡脚本支持通过 `DOCKER_CONTAINERS`（逗号分隔）采集容器 `docker stats`，用于观察资源曲线。


### 需求: 脚本说明补全与目录整理
**模块:** scripts
补充脚本用途与用法注释，并将生成视频目录整合到 scripts。

#### 场景: 脚本用途快速识别
打开脚本即可看到用途与启动方式。
- 预期结果: 每个脚本文件开头包含用途与用法说明

## API接口
脚本通过调用后端API实现演示/验收，具体接口以 `wiki/api.md` 为准。

## 数据模型
脚本不直接定义数据模型。

## 依赖
- backend

## 变更历史
- 202601210936_batch_video_import - 批量导入视频脚本（CSV/JSON + 自动审核）
- 202601181015_m1_6_rule_reco_demo（如有对应记录，以 `history/` 为准）
- 202601221528_jmeter-stress-enhancement - JMeter 混合/摸高/浸泡压测脚本与报告
