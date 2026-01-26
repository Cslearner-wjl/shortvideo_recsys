# 技术设计: Spark 3.1.x + Java 17 兼容性修复（项目内固定 Spark 3.5.1）

## 技术方案

### 核心技术
- Bash 脚本（下载/校验/解压/缓存）
- Spark 3.5.1 二进制发行版（`spark-3.5.1-bin-hadoop3`）
- 统一启动包装：`spark-submit` 参数与 Java 17 兼容参数注入

### 实现要点
- **目录规划：** 将 Spark 发行版缓存到项目目录（例如 `bigdata/streaming/.spark/` 或 `./.cache/spark/`），避免污染系统环境。
- **可配置：**
  - `SPARK_VERSION` 默认 `3.5.1`
  - `SPARK_DIST` 可直接指定已解压的 Spark 目录
  - `SPARK_MIRROR`/`SPARK_TGZ_URL` 支持自定义下载源（含内网镜像）
- **校验与幂等：**
  - 下载后优先校验 SHA512（如可获取）；否则至少校验文件存在与解压目录结构
  - 重复执行脚本不重复下载
- **Java 17 兼容：**
  - 统一在脚本中设置 `SPARK_SUBMIT_OPTS` 或注入 `spark.driver.extraJavaOptions` / `spark.executor.extraJavaOptions`
  - 默认包含 `--add-opens=java.base/java.nio=ALL-UNNAMED`、`--add-opens=java.base/sun.nio.ch=ALL-UNNAMED`
  - 保留用户自定义的 JVM 参数（append 而非覆盖）
- **依赖与 classpath：**
  - `bigdata/streaming` 的 `pom.xml` 继续保持 Spark 相关依赖 `provided`，由 Spark 发行版的 `jars/` 提供运行时类
  - 启动脚本默认使用该发行版的 `bin/spark-submit`，确保与依赖版本一致

## 架构决策 ADR

### ADR-001: 采用项目内固定 Spark 发行版（推荐方案）
**上下文:** 系统 Spark 为 3.1.x 且运行在 Java 17 时存在兼容性问题；同时需要避免影响系统/集群 Spark 的其他任务。  
**决策:** 在仓库内提供脚本下载并固定 Spark 3.5.1 发行版，并通过包装脚本统一使用该发行版运行 streaming 作业。  
**理由:** 可控、可复现、对外部环境依赖最小，符合“避免其他兼容性问题”的目标。  
**替代方案:**  
- 方案B：升级系统/集群 Spark 到 3.5.x → 拒绝原因: 影响范围不可控、可能波及其他任务/作业。  
- 方案C：让 Spark 3.1.x 使用 Java 11 运行 → 拒绝原因: 需要维护双 Java 运行时策略，且仍存在 Spark 3.1 其他已知兼容性风险。  
**影响:** 仓库新增脚本与缓存目录规则；需要更新文档并确保缓存目录不入库。

## 安全与性能
- **安全:** 不在仓库中引入密钥；脚本仅处理公开发行版下载与本地解压；对自定义下载 URL 进行基础校验（空值/路径合法性）。
- **性能:** 发行版缓存后重复运行不再下载；解压只在首次安装时发生。

## 测试与部署
- **测试:**
  - 构建：`mvn -f bigdata/streaming/pom.xml -DskipTests package`
  - 运行：使用新增脚本拉起 `spark-submit`（local 模式），观察作业是否正常启动与持续消费
- **部署:**
  - 本地/开发机：脚本自动下载 Spark
  - CI/离线环境：通过 `SPARK_DIST` 指向预置 Spark 目录，跳过下载

