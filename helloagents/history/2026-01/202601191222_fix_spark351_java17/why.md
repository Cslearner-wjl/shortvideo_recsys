# 变更提案: Spark 3.1.x + Java 17 兼容性修复（项目内固定 Spark 3.5.1）

## 需求背景
当前运行 `bigdata/streaming`（Spark Structured Streaming 作业）时，使用 Spark 3.1.x 且运行在 Java 17 环境下出现兼容性问题，导致作业无法正常启动或运行。

本变更希望在**不影响系统/集群 Spark 安装**的前提下，通过在项目内固定并使用 Spark 3.5.1（对 Java 17 兼容更好）来解决问题，并降低升级带来的额外兼容性风险。

## 变更内容
1. 在项目内提供脚本：自动下载/校验/缓存 Spark 3.5.1 二进制发行版，并提供统一的 `spark-submit` 启动入口。
2. 对 `bigdata/streaming` 提供“一键构建 + 运行”示例，确保默认参数适配 Java 17（必要时包含 `--add-opens`）。
3. 更新仓库文档与知识库，明确本模块的运行方式与常见兼容性注意事项。

## 影响范围
- **模块:** `bigdata/streaming`、`scripts/`、`docs/`、`helloagents/wiki/`
- **文件:** 预计新增/修改运行脚本、`bigdata/streaming/README.md`、知识库模块文档与变更记录
- **API:** 无
- **数据:** 无（仅运行时依赖与启动方式变更）

## 核心场景

### 需求: 使用项目内固定 Spark 3.5.1 运行 streaming
**模块:** bigdata
提供项目内脚本固定 Spark 版本，避免依赖本机 Spark 版本与 Java 兼容性差异。

#### 场景: 本机仅有 Java 17，系统 Spark 为 3.1.x
用户使用脚本启动 `spark-submit`，无需升级系统 Spark，即可运行 `BehaviorStreamingJob`。
- 作业可正常启动并持续运行
- Kafka/Redis 连接参数仍可通过环境变量覆盖

### 需求: 避免引入其他兼容性问题
**模块:** bigdata
升级仅影响 streaming 作业运行时，不改变构建产物的依赖策略（Spark 依赖保持 `provided`），并提供可回退/可覆盖配置。

#### 场景: 用户需要自定义 Spark 发行版或离线环境
脚本支持通过环境变量指定下载地址/已存在的 Spark 目录，避免固定单一网络源。
- 可配置 `SPARK_VERSION`、`SPARK_DIST` 或 `SPARK_HOME`
- 失败时给出明确报错与排查提示

## 风险评估
- **风险:** Spark 发行版下载源不可用、平台差异（Linux/macOS）、脚本依赖工具缺失（curl/tar）
- **缓解:** 支持多下载工具与自定义镜像；下载后做校验；对缺失依赖给出明确提示；目录缓存并默认加入 `.gitignore`

