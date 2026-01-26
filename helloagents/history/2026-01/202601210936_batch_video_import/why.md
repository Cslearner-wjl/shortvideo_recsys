# 变更提案: 批量导入视频脚本

## 需求背景
当前系统已有管理端上传接口与 MinIO 存储，但缺少可重复执行的批量导入脚本，导致真实视频素材难以快速导入，影响用户端播放验证与推荐链路演示。

## 产品分析

### 目标用户与场景
- **用户群体:** 研发/测试/演示人员
- **使用场景:** 本地或测试环境中批量导入真实视频素材，快速构建可播放数据集
- **核心痛点:** 单次上传效率低、导入流程不规范、数据可复用性差

### 价值主张与成功指标
- **价值主张:** 通过脚本化导入降低素材接入成本，提高可重复验证效率
- **成功指标:** 单次执行可导入多条视频并能在用户端播放；导入失败可定位原因

### 人文关怀
避免在非受控环境暴露弱口令；仅在测试/演示环境使用预置账号，支持通过环境变量覆盖。

## 变更内容
1. 新增批量导入脚本（bash），支持 CSV/JSON 输入
2. 增加上传账号预置（Flyway 迁移），便于脚本固定 uploaderUserId
3. 完善脚本使用说明与接口约束

## 影响范围
- **模块:** scripts、backend、docs/knowledge
- **文件:** scripts/、backend/src/main/resources/db/migration、helloagents/wiki/modules/scripts.md、helloagents/wiki/modules/backend.md、helloagents/wiki/api.md
- **API:** 复用 `POST /api/admin/videos`、`PATCH /api/admin/videos/{id}/audit`
- **数据:** 新增 users 预置记录（仅测试/演示环境）

## 核心场景

### 需求: 批量导入真实视频
**模块:** scripts
提供 bash 脚本按 CSV/JSON 批量上传视频文件，并在必要时自动审核通过。

#### 场景: 通过管理端接口上传并落 MinIO
前置条件：后端 docker/test profile 已启动；管理端 Basic Auth 可用。
- 脚本读取 CSV/JSON，解析文件路径与元数据
- 逐条调用 `POST /api/admin/videos` 上传
- 成功后可选调用 `PATCH /api/admin/videos/{id}/audit` 置为 APPROVED

### 需求: 预置上传用户
**模块:** backend
提供固定用户名 uploaduser，用于脚本 uploaderUserId。

#### 场景: Flyway 迁移初始化 uploaduser
前置条件：数据库可用，Flyway 启用（docker/test）。
- 若用户不存在则插入
- 若已存在则保持不变

## 风险评估
- **风险:** 固定弱口令存在安全隐患
- **缓解:** 限定在 docker/test 环境；允许通过环境变量覆盖或后续修改
