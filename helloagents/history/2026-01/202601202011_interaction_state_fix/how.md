# 技术设计: 评论一致性与点赞收藏切换

## 技术方案
### 核心技术
- Java / Spring Boot / MyBatis-Plus
- Vue 3 / Vite / TypeScript

### 实现要点
- 后端为视频列表/详情/推荐响应补充 liked、favorited 字段，基于当前用户批量查询 video_likes 与 video_favorites。
- 前端扩展 VideoItem 模型并在 Feed/VideoDetail 中维护 liked/favorited 状态；点击时根据状态调用 POST/DELETE，并回滚计数。
- 评论面板打开后，使用评论接口 total 同步当前视频 commentCount，确保展示一致。

## API设计
### GET /api/videos/{id}
- **响应新增:** liked: boolean, favorited: boolean

### GET /api/videos/page
- **响应新增:** items[*].liked, items[*].favorited

### GET /api/recommendations
- **响应新增:** items[*].liked, items[*].favorited

## 安全与性能
- **安全:** 依旧依赖现有鉴权；未登录场景返回 liked/favorited=false；点赞/收藏取消仍使用后端鉴权校验。
- **性能:** 对列表接口使用批量查询，避免 N+1；对单条视频仅查询一次状态。

## 测试与部署
- **测试:** 补充后端接口返回 liked/favorited 的集成测试；前端进行手工回归（点赞/收藏切换、评论数量一致性）。
- **部署:** 仅代码变更，无数据库迁移。
