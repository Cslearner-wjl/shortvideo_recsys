# 任务清单: 脚本注释与文档迁移整理

目录: `helloagents/plan/202601211137_cleanup_scripts_docs/`

---

## 1. 脚本注释（根目录与后端）
- [√] 1.1 在 `simple_test.sh` 与 `test_events.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 1.2 在 `test_behavior_logging.sh` 与 `run_validation.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 1.3 在 `check_m31_environment.sh` 与 `restart_backend.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 1.4 在 `backend/run_backend_18080.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别

## 2. 脚本注释（bigdata）
- [√] 2.1 在 `bigdata/batch/bin/ensure_spark.sh` 与 `bigdata/batch/bin/run_als.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 2.2 在 `bigdata/streaming/bin/ensure_spark.sh` 与 `bigdata/streaming/bin/run_streaming.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别

## 3. 脚本注释（scripts）
- [√] 3.1 在 `scripts/acceptance_m3_2_streaming.sh` 与 `scripts/run_m3_2_streaming.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 3.2 在 `scripts/verify_demo_recommendations.sh` 与 `scripts/verify_behavior_events.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 3.3 在 `scripts/run_m4_als_batch.sh` 与 `scripts/acceptance_m4_als.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 3.4 在 `scripts/batch_import_videos.sh` 与 `scripts/crawl_and_import.sh` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 3.5 在 `scripts/run_backend_test_18080.sh` 与 `scripts/acceptance_hot_rank.ps1` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别
- [√] 3.6 在 `scripts/demo_recommendations.ps1` 中补充用途与用法注释，验证 why.md#需求-脚本说明补全-场景-脚本用途快速识别

## 4. 目录整理
- [√] 4.1 将 `生成视频/` 迁移至 `scripts/生成视频/` 并更新引用（如有），验证 why.md#需求-目录整理-场景-生成视频目录合并

## 5. 文档迁移与标识
- [√] 5.1 新建 `docs/运行指南.md`，迁移 README 中依赖/后端/前端启动与常见排查说明，验证 why.md#需求-文档迁移与标识-场景-启动指南集中
- [√] 5.2 新建 `docs/脚本说明.md`，集中说明关键脚本（含 `scripts/crawl_and_import.sh`），验证 why.md#需求-文档迁移与标识-场景-启动指南集中
- [√] 5.3 更新 `README.md` 保留简短入口与链接到 docs，验证 why.md#需求-文档迁移与标识-场景-启动指南集中

## 6. 安全检查
- [√] 6.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 7. 测试
- [√] 7.1 自查关键启动命令与脚本路径无误（不强制运行）
