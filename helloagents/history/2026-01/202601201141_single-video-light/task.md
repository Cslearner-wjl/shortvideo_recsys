# 任务清单: 单视频播放模式与浅色风格回退

目录: `helloagents/plan/202601201141_single-video-light/`

---

## 1. 用户端单视频模式与键盘切换
- [√] 1.1 在 `frontend/user/src/pages/Feed.vue` 中改为单视频渲染与上下键切换，验证 why.md#需求:-单视频播放模式-场景:-用户浏览视频
- [√] 1.2 在 `frontend/user/src/pages/Hot.vue` 中改为单视频渲染与上下键切换，验证 why.md#需求:-单视频播放模式-场景:-用户浏览视频
- [√] 1.3 在 `frontend/user/src/pages/Feed.vue`/`Hot.vue` 中实现切换即播放逻辑，验证 why.md#需求:-键盘切换即播放-场景:-键盘操作

## 2. 浅色主题回退与中文文案
- [√] 2.1 在 `frontend/user/src/styles/base.css` 中恢复浅色主题并更新文案样式，验证 why.md#需求:-浅色主题与中文文案-场景:-页面展示
- [√] 2.2 在 `frontend/admin/src/styles/base.css` 与 `frontend/admin/src/styles/layout.css` 中恢复浅色主题，验证 why.md#需求:-浅色主题与中文文案-场景:-页面展示
- [√] 2.3 在 `frontend/user/src/App.vue`、`Feed.vue`、`Hot.vue`、组件中替换中文文案，验证 why.md#需求:-浅色主题与中文文案-场景:-页面展示
- [√] 2.4 在 `frontend/admin/src/layouts/AdminLayout.vue` 与 `frontend/admin/src/pages/Login.vue` 中替换中文文案，验证 why.md#需求:-浅色主题与中文文案-场景:-页面展示

## 3. 安全检查
- [√] 3.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 4. 文档更新
- [√] 4.1 更新 `helloagents/wiki/modules/frontend-user.md`
- [√] 4.2 更新 `helloagents/wiki/modules/frontend-admin.md`
- [√] 4.3 更新 `helloagents/wiki/arch.md`（补充 ADR-003）

## 5. 测试
- [-] 5.1 手工验证：推荐/榜单单视频切换与自动播放
> 备注: 未执行手工验证
