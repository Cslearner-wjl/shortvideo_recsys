# 变更提案: 评论一致性与点赞收藏切换

## 需求背景
当前视频卡片/详情页显示评论数，但打开评论面板时出现“有数量但无列表”的不一致；同时点赞与收藏需要支持“再次点击取消”的交互与可视状态反馈。

## 变更内容
1. 后端视频列表/详情/推荐接口补充 liked、favorited 状态，前端据此渲染并支持切换。
2. 评论面板打开后以评论接口 total 同步 commentCount，保证展示数量与列表一致。
3. 点赞/收藏 UI 状态：未操作为灰色，点赞后爱心变红，收藏后星标变黄；失败需回滚。

## 影响范围
- **模块:** backend/video, backend/recommendation, frontend-user
- **文件:** 后端 DTO/Service/Controller，前端 services 与页面组件
- **API:** GET /api/videos/{id}, GET /api/videos/page, GET /api/recommendations（新增 liked/favorited 字段）
- **数据:** 无新增表结构（复用 video_likes, video_favorites）

## 核心场景

### 需求: 评论展示一致性
**模块:** frontend-user
评论数量与评论列表展示保持一致。

#### 场景: 打开评论面板
用户点击评论按钮打开面板。
- 面板标题中的数量与接口返回 total 一致
- 若 total=0 显示“暂无评论”

### 需求: 点赞/收藏可切换与状态反馈
**模块:** frontend-user / backend-video
用户可以重复点击进行取消，并获得明确的颜色反馈。

#### 场景: 首次点击与二次点击
用户点击点赞/收藏按钮。
- 首次点击触发 POST，状态变为已点赞/已收藏并变色
- 再次点击触发 DELETE，状态恢复未点赞/未收藏并变灰
- 请求失败时状态与计数回滚，并提示失败

## 风险评估
- **风险:** API 返回字段变更导致前端兼容问题
- **缓解:** 前端默认 false + 兼容旧字段；后端保持字段新增不破坏已有响应结构
