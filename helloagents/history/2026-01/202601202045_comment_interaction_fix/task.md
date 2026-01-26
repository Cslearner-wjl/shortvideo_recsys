# 任务清单: 评论交互修复与布局调整

目录: `helloagents/plan/202601202045_comment_interaction_fix/`

---

## 1. 评论输入与发送恢复
- [√] 1.1 在 `frontend/user/src/pages/Feed.vue` 中确保 CommentBox 输入区与发送按钮可见，验证 why.md#需求-评论输入与发送恢复-场景-发表评论
- [√] 1.2 在 `frontend/user/src/pages/Hot.vue` 中接入 CommentBox 并显示输入区与发送按钮，验证 why.md#需求-评论输入与发送恢复-场景-发表评论
- [√] 1.3 在 `frontend/user/src/components/CommentBox.vue` 中检查输入区显示条件与样式，确保按钮可见，验证 why.md#需求-评论输入与发送恢复-场景-发表评论

## 2. 热门页面按钮可用性修复
- [√] 2.1 在 `frontend/user/src/pages/Hot.vue` 中修复互动按钮不可点击问题，验证 why.md#需求-热门页面按钮可用-场景-互动按钮点击

## 3. 详情按钮位置调整
- [√] 3.1 在 `frontend/user/src/pages/Feed.vue` 中调整详情按钮位置到点赞按钮上方，验证 why.md#需求-详情按钮位置调整-场景-页面布局
- [√] 3.2 在 `frontend/user/src/pages/Hot.vue` 中调整详情按钮位置到点赞按钮上方，验证 why.md#需求-详情按钮位置调整-场景-页面布局

## 4. 安全检查
- [√] 4.1 执行安全检查（按G9: 输入验证、敏感信息处理、权限控制、EHRB风险规避）

## 5. 文档更新
- [√] 5.1 更新 `helloagents/wiki/modules/frontend-user.md`

## 6. 测试
- [√] 6.1 手工回归：Feed/Hot 评论发布、详情按钮位置、互动按钮点击
