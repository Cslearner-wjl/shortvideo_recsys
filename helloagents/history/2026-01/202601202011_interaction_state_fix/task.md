# 任务清单: 评论一致性与点赞收藏切换

目录: `helloagents/plan/202601202011_interaction_state_fix/`

---

## 1. 后端视频详情与分页状态
- [√] 1.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/video/dto/VideoDto.java` 中新增 liked、favorited 字段，验证 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击
- [√] 1.2 在 `backend/src/main/java/com/shortvideo/recsys/backend/video/VideoService.java` 中批量加载点赞/收藏状态并填充 VideoDto，验证 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击，依赖任务1.1
- [√] 1.3 在 `backend/src/main/java/com/shortvideo/recsys/backend/video/VideoController.java` 中传入 viewerId/鉴权上下文以便返回状态，验证 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击，依赖任务1.2

## 2. 后端推荐列表状态
- [√] 2.1 在 `backend/src/main/java/com/shortvideo/recsys/backend/recommendation/dto/RecommendationVideoDto.java` 中新增 liked、favorited 字段，验证 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击
- [√] 2.2 在 `backend/src/main/java/com/shortvideo/recsys/backend/recommendation/RecommendationService.java` 中批量查询点赞/收藏并注入 DTO，验证 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击，依赖任务2.1

## 3. 前端数据模型与接口
- [√] 3.1 在 `frontend/user/src/services/video.ts` 中扩展 VideoItem 的 liked/favorited，并新增 unlikeVideo/unfavoriteVideo 方法，验证 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击

## 4. 前端评论一致性与交互切换
- [√] 4.1 在 `frontend/user/src/components/CommentBox.vue` 中在加载后抛出评论 total，用于同步 commentCount，验证 why.md#需求-评论展示一致性-场景-打开评论面板
- [√] 4.2 在 `frontend/user/src/pages/Feed.vue` 中实现点赞/收藏切换与颜色状态，并接收评论 total 同步 commentCount，验证 why.md#需求-评论展示一致性-场景-打开评论面板 与 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击，依赖任务3.1与4.1
- [√] 4.3 在 `frontend/user/src/pages/VideoDetail.vue` 中实现点赞/收藏切换与颜色状态，并接收评论 total 同步 commentCount，验证 why.md#需求-评论展示一致性-场景-打开评论面板 与 why.md#需求-点赞收藏可切换与状态反馈-场景-首次点击与二次点击，依赖任务3.1与4.1
- [√] 4.4 在 `frontend/user/src/pages/Hot.vue` 中使用评论接口 total 同步 commentCount，验证 why.md#需求-评论展示一致性-场景-打开评论面板

## 5. 安全检查
- [√] 5.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 6. 文档更新
- [√] 6.1 更新 `helloagents/wiki/modules/frontend-user.md`
- [√] 6.2 更新 `helloagents/wiki/modules/backend.md`

## 7. 测试
- [√] 7.1 在 `backend/src/test/java/com/shortvideo/recsys/backend/video/VideoLikeFlowIntegrationTest.java` 中补充 liked/favorited 字段的返回断言
- [√] 7.2 在 `backend/src/test/java/com/shortvideo/recsys/backend/recommendation/RecommendationIntegrationTest.java` 中补充推荐列表 liked/favorited 返回断言
