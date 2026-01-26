# 任务清单: M2.2 用户前端基础页面与联调

目录: `helloagents/plan/202601181158_m2_2_user_frontend/`

---

## 1. 路由与页面骨架
- [√] 1.1 在 `frontend/user/src/router/index.ts` 中实现用户端路由，覆盖登录/注册/视频流/热门榜，验证 why.md#需求-登录与注册-场景-登录
- [√] 1.2 在 `frontend/user/src/main.ts` 与 `frontend/user/src/App.vue` 中接入路由与基础布局，验证 why.md#需求-视频流与播放-场景-浏览视频流

## 2. API client 与鉴权
- [√] 2.1 在 `frontend/user/src/services/http.ts` 与 `frontend/user/src/services/token.ts` 中实现 baseURL + token 注入，验证 why.md#需求-登录与注册-场景-登录
- [√] 2.2 在 `frontend/user/src/services/auth.ts`、`frontend/user/src/services/video.ts`、`frontend/user/src/services/rank.ts` 中封装接口，验证 why.md#需求-热门榜-场景-查看热门榜

## 3. 页面实现
- [√] 3.1 在 `frontend/user/src/pages/Login.vue` 实现登录页与错误提示，验证 why.md#需求-登录与注册-场景-登录
- [√] 3.2 在 `frontend/user/src/pages/Register.vue` 实现注册页与验证码倒计时，验证 why.md#需求-登录与注册-场景-注册与验证码
- [√] 3.3 在 `frontend/user/src/pages/Feed.vue` 实现视频流与播放上报，验证 why.md#需求-视频流与播放-场景-播放上报
- [√] 3.4 在 `frontend/user/src/pages/Hot.vue` 实现热门榜展示，验证 why.md#需求-热门榜-场景-查看热门榜

## 4. 组件与交互
- [√] 4.1 在 `frontend/user/src/components/VideoCard.vue` 中整合视频信息与播放按钮，验证 why.md#需求-视频流与播放-场景-浏览视频流
- [√] 4.2 在 `frontend/user/src/components/ReactionBar.vue` 与 `frontend/user/src/components/CommentBox.vue` 中实现点赞/收藏/评论交互，验证 why.md#需求-互动组件-场景-互动操作

## 5. 样式与说明
- [√] 5.1 在 `frontend/user/src/styles/base.css` 中提供最小样式并在入口引入
- [√] 5.2 在 `frontend/user/src/pages/*.vue` 中补充必要样式，验证 why.md#需求-视频流与播放-场景-浏览视频流

## 6. 安全检查
- [√] 6.1 执行安全检查（输入校验、token 存储、接口权限、EHRB 风险规避）

## 7. 文档更新
- [√] 7.1 更新 `helloagents/wiki/modules/frontend-user.md` 记录模块变更

## 8. 测试
- [-] 8.1 执行 `frontend/user/package.json` 中的 dev 运行与手动冒烟验证（登录/注册/视频流/互动/热门榜）
  > 备注: 未执行，需手动验证
