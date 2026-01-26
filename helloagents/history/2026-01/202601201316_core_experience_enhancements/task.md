# 任务清单: 核心功能完善与体验增强

目录: `helloagents/plan/202601201316_core_experience_enhancements/`

---

## 1. 核心功能 - 用户与评论
- [√] 1.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/user/UserController.java` 中新增用户信息修改接口，验证 why.md#需求:-用户信息修改-场景:-修改个人资料
- [√] 1.2 在 `frontend/user/src/pages/` 新增个人信息页面并对接用户修改接口，验证 why.md#需求:-用户信息修改-场景:-修改个人资料，依赖任务1.1
- [√] 1.3 在 `backend/src/main/java/com/shortvideo/recsys/backend/video/CommentController.java` 中完善评论列表查询返回字段（含用户信息/点赞状态），验证 why.md#需求:-评论列表查询与展示-场景:-浏览评论列表
- [√] 1.4 在 `frontend/user/src/components/CommentBox.vue` 中完善评论列表展示与分页加载，验证 why.md#需求:-评论列表查询与展示-场景:-浏览评论列表，依赖任务1.3

## 2. 核心功能 - 管理员账号管理
- [√] 2.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/admin/AdminUserController.java` 中新增管理员账号增删改改密接口，验证 why.md#需求:-管理员账号管理-场景:-管理员新增与改密
- [√] 2.2 在 `frontend/admin/src/pages/Admins.vue` 中完善管理员账号管理 UI 与接口对接，验证 why.md#需求:-管理员账号管理-场景:-管理员新增与改密

## 3. 用户体验 - 视频详情与交互
- [√] 3.1 在 `frontend/user/src/pages/` 新增视频详情页并接入评论区组件，验证 why.md#需求:-视频详情页与评论区-场景:-浏览视频详情
- [√] 3.2 在 `frontend/user/src/pages/Feed.vue` 中优化上下滑切换逻辑，验证 why.md#需求:-上下滑交互优化-场景:-连续刷视频
- [√] 3.3 在 `frontend/user/src/services/video.ts` 中补充播放时长/完播上报触发逻辑，验证 why.md#需求:-播放时长/完播上报-场景:-播放上报

## 4. 用户体验 - 统计图表
- [√] 4.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/admin/AdminAnalyticsController.java` 中新增视频发布量统计接口，验证 why.md#需求:-视频发布量统计图表-场景:-统计图表展示
- [√] 4.2 在 `frontend/admin/src/pages/Dashboard.vue` 中新增视频发布量图表组件，验证 why.md#需求:-视频发布量统计图表-场景:-统计图表展示，依赖任务4.1

## 5. 可选增强
- [-] 5.1 在 `frontend/user/src/components/ReactionBar.vue` 中完善取消点赞/收藏交互，验证 why.md#需求:-可选增强-场景:-取消点赞/收藏
  > 备注: 当前列表未返回点赞/收藏状态，暂不扩展取消交互以避免状态误判。
- [-] 5.2 在 `backend/src/main/java/com/shortvideo/recsys/backend/admin/AdminAnalyticsController.java` 中新增推荐点击率统计接口（若已有数据链路），验证 why.md#需求:-可选增强
  > 备注: 暂无推荐点击率数据链路与统计表，暂不实现。
- [-] 5.3 在 `frontend/user/src/pages/` 完善视频封面上传流程（用户端或管理端按现有设计），验证 why.md#需求:-可选增强
  > 备注: 当前上传接口未包含封面字段，暂不扩展上传流程。

## 6. 安全检查
- [√] 6.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 7. 文档更新
- [√] 7.1 更新 `helloagents/wiki/api.md` 记录新增接口
- [√] 7.2 更新 `helloagents/wiki/modules/backend.md`
- [√] 7.3 更新 `helloagents/wiki/modules/frontend-user.md`
- [√] 7.4 更新 `helloagents/wiki/modules/frontend-admin.md`

## 8. 测试
- [√] 8.1 在 `backend/src/test/java/com/shortvideo/recsys/backend/admin/AdminUserIntegrationTest.java` 中新增管理员管理测试
- [√] 8.2 在 `backend/src/test/java/com/shortvideo/recsys/backend/user/` 新增用户修改接口测试
