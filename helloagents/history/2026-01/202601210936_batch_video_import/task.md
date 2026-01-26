# 任务清单: 批量导入视频脚本

目录: `helloagents/plan/202601210936_batch_video_import/`

---

## 1. 批量导入脚本
- [√] 1.1 在 `scripts/batch_import_videos.sh` 中实现 CSV/JSON 解析与上传逻辑，验证 why.md#需求-批量导入真实视频-场景-通过管理端接口上传并落-minio
- [√] 1.2 在 `scripts/batch_import_videos.sh` 中实现可选自动审核与日志输出，验证 why.md#需求-批量导入真实视频-场景-通过管理端接口上传并落-minio，依赖任务1.1

## 2. 预置上传用户
- [√] 2.1 在 `backend/src/main/resources/db/migration/V4__seed_upload_user.sql` 中新增 uploaduser 迁移，验证 why.md#需求-预置上传用户-场景-flyway-迁移初始化-uploaduser

## 3. 安全检查
- [√] 3.1 执行安全检查（输入验证、敏感信息处理、权限控制、弱口令风险说明）

## 4. 文档更新
- [√] 4.1 更新 `helloagents/wiki/modules/scripts.md` 记录脚本用途与参数
- [√] 4.2 更新 `helloagents/wiki/modules/backend.md` 与 `helloagents/wiki/api.md` 说明 uploaduser 预置与导入建议

## 5. 测试
- [-] 5.1 运行本地脚本单条上传验证（可选），验证返回 videoId 并能在用户端播放
> 备注: 未执行，本地环境与素材未提供
