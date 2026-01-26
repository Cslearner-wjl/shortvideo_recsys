# 任务清单: 互动对接与持久化校验

目录: `helloagents/plan/202601202105_interaction_persistence_fix/`

---

## 1. 登录态与请求对接
- [√] 1.1 在 `frontend/user/src/services/token.ts` 中确认登录态获取逻辑（如需补充），验证 why.md#需求-未登录限制写操作-场景-未登录点击
- [√] 1.2 在 `frontend/user/src/services/http.ts` 中统一 401/403 处理与提示策略，验证 why.md#需求-未登录限制写操作-场景-未登录点击

## 2. 页面写操作对接
- [√] 2.1 在 `frontend/user/src/pages/Feed.vue` 中统一评论/点赞/收藏写入逻辑与失败回滚，验证 why.md#需求-登录后评论落库-场景-评论提交 与 why.md#需求-登录后点赞/收藏落库-场景-点赞/收藏切换
- [√] 2.2 在 `frontend/user/src/pages/Hot.vue` 中统一评论/点赞/收藏写入逻辑与失败回滚，验证 why.md#需求-登录后评论落库-场景-评论提交 与 why.md#需求-登录后点赞/收藏落库-场景-点赞/收藏切换
- [√] 2.3 在 `frontend/user/src/pages/VideoDetail.vue` 中统一评论/点赞/收藏写入逻辑与失败回滚，验证 why.md#需求-登录后评论落库-场景-评论提交 与 why.md#需求-登录后点赞/收藏落库-场景-点赞/收藏切换

## 3. 安全检查
- [√] 3.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 4. 文档更新
- [√] 4.1 更新 `helloagents/wiki/modules/frontend-user.md`

## 5. 测试
- [√] 5.1 手工回归：登录后评论/点赞/收藏成功并落库（MySQL 表 `comments`/`video_likes`/`video_favorites`）
