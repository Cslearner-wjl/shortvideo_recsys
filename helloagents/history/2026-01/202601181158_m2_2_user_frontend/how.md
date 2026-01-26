# 技术设计: M2.2 用户前端基础页面与联调

## 技术方案
### 核心技术
- Vue 3 + Vite + TypeScript
- Vue Router（新增依赖）
- 原生 Fetch 封装 API client

### 实现要点
- 路由结构: /login、/register、/feed、/hot，默认重定向到 /feed 或 /login（取决于登录态）
- 页面拆分: Login/Register/Feed/Hot 页面 + VideoCard/ReactionBar/CommentBox 组件
- API client: 统一 request 方法，自动拼接 baseURL、注入 token、统一错误处理
- token 存储: localStorage 持久化 + 内存缓存，统一在 token 工具中读写
- 交互更新: 互动操作采用乐观更新，本地计数先变更，失败时回滚

## API 设计
### 登录与注册（待对齐后端实现）
- POST /api/auth/login
- POST /api/auth/register
- POST /api/auth/code（获取验证码）

### 推荐与视频
- GET /api/recommendations 或 /api/videos/feed
- POST /api/videos/{id}/play

### 互动
- POST /api/videos/{id}/like
- POST /api/videos/{id}/favorite
- POST /api/videos/{id}/comment

### 排行榜
- GET /api/rank/hot

## 安全与性能
- **安全:** token 统一存取，避免在组件中散落处理；登出时清理 localStorage
- **性能:** 列表渲染只展示必要字段，避免深层响应式对象

## 测试与部署
- **测试:** 以手动冒烟为主（登录、注册、视频流、互动、热门榜）
- **部署:** 前端本地开发通过 Vite dev server 运行
