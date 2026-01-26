# 任务清单: Spark 3.1.x + Java 17 兼容性修复（项目内固定 Spark 3.5.1）

目录: `helloagents/history/2026-01/202601191222_fix_spark351_java17/`

---

## 1. bigdata（Streaming 启动链路）
- [√] 1.1 新增项目内 Spark 安装脚本（下载/校验/缓存/幂等），验证 why.md#需求-使用项目内固定-spark-351-运行-streaming-场景-本机仅有-java-17系统-spark-为-31x
- [√] 1.2 新增 streaming 运行包装脚本（统一 spark-submit + Java 17 参数注入 + 环境变量透传），验证 why.md#需求-使用项目内固定-spark-351-运行-streaming-场景-本机仅有-java-17系统-spark-为-31x，依赖任务1.1
- [√] 1.3 更新 `bigdata/streaming/README.md`：默认引导使用新脚本运行，并说明回退/覆盖方式，验证 why.md#需求-避免引入其他兼容性问题-场景-用户需要自定义-spark-发行版或离线环境，依赖任务1.2

## 2. scripts（统一入口，可选）
- [√] 2.1 如仓库已有统一脚本规范，则在 `scripts/` 下增加聚合入口或软链接说明，验证 why.md#需求-使用项目内固定-spark-351-运行-streaming-场景-本机仅有-java-17系统-spark-为-31x

## 3. 安全检查
- [√] 3.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 4. 文档更新（知识库）
- [√] 4.1 更新 `helloagents/wiki/modules/bigdata.md`：补充“项目内固定 Spark 3.5.1 的推荐运行方式”与常见兼容性说明
- [√] 4.2 更新 `helloagents/CHANGELOG.md` 记录本次变更

## 5. 测试
- [√] 5.1 本地构建 streaming jar：`mvn -f bigdata/streaming/pom.xml -DskipTests package`，验证构建可通过
- [√] 5.2 运行脚本 smoke test：验证脚本可拉起 `spark-submit`（`--spark-version`）且 Java 17 参数注入生效
