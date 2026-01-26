# 任务清单: 管理端黑灰风格与用户端推荐/榜单增强

目录: `helloagents/plan/202601201045_ui-admin-userfeed/`

---

## 1. 管理端黑灰统一风格
- [√] 1.1 在 `frontend/admin/src/styles/base.css` 中定义黑灰主题变量与基础排版，验证 why.md#需求:-管理端黑灰极简风-场景:-管理端页面访问
- [√] 1.2 在 `frontend/admin/src/styles/layout.css` 中统一布局、卡片与表格视觉，验证 why.md#需求:-管理端黑灰极简风-场景:-管理端页面访问
- [√] 1.3 在 `frontend/admin/src/pages/Login.vue` 中调整登录页布局与视觉层级，验证 why.md#需求:-管理端黑灰极简风-场景:-管理端页面访问

## 2. 用户端推荐/榜单与卡片对齐
- [√] 2.1 在 `frontend/user/src/pages/Feed.vue` 中加入筛选/排序/空态/无限滚动结构，验证 why.md#需求:-用户端推荐/榜单能力增强-场景:-用户筛选与浏览
- [√] 2.2 在 `frontend/user/src/pages/Hot.vue` 中加入筛选/排序/统计扩展与空态，验证 why.md#需求:-用户端推荐/榜单能力增强-场景:-用户筛选与浏览
- [√] 2.3 在 `frontend/user/src/components/VideoCard.vue` 中实现字段栅格对齐布局，验证 why.md#需求:-视频卡片字段对齐-场景:-用户浏览卡片

## 3. 失败弹窗与重试
- [√] 3.1 在 `frontend/user/src/pages/Feed.vue` 中引入弹窗与重试逻辑，验证 why.md#需求:-播放/互动失败弹窗重试-场景:-操作失败
- [√] 3.2 在 `frontend/user/src/styles/base.css` 中补充弹窗样式，验证 why.md#需求:-播放/互动失败弹窗重试-场景:-操作失败

## 4. 评论列表 API 与互动字段
- [√] 4.1 在 `backend/src/main/resources/db/migration` 中新增评论点赞字段与表迁移，验证 why.md#需求:-评论列表-API-场景:-用户端/管理端获取评论
- [√] 4.2 在 `backend/src/main/java/com/shortvideo/recsys/backend/video` 中新增评论列表/点赞接口与服务逻辑，验证 why.md#需求:-评论列表-API-场景:-用户端/管理端获取评论
- [√] 4.3 在 `frontend/user/src/services` 中新增评论列表/点赞请求，验证 why.md#需求:-评论列表-API-场景:-用户端/管理端获取评论

## 5. 安全检查
- [√] 5.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 6. 文档更新
- [√] 6.1 更新 `helloagents/wiki/api.md`
- [√] 6.2 更新 `helloagents/wiki/data.md`
- [√] 6.3 更新 `helloagents/wiki/modules/frontend-admin.md` 与 `helloagents/wiki/modules/frontend-user.md`（如存在）
- [-] 6.4 更新 `helloagents/wiki/modules/backend-video.md`（如存在）
> 备注: 目标文件不存在，已在 `helloagents/wiki/modules/backend.md` 补充相关说明

## 7. 测试
- [√] 7.1 在 `backend/src/test/java` 中新增评论列表/点赞接口测试场景，验证分页与字段差异
