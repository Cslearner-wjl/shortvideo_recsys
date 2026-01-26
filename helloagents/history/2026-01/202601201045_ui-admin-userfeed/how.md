# 技术设计: 管理端黑灰风格与用户端推荐/榜单增强

## 技术方案
### 核心技术
- Vue 3 + Vite + TypeScript
- Element Plus（管理端基础组件）
- 后端 Spring Boot + MyBatis-Plus

### 实现要点
- 管理端统一主题变量（黑灰）与布局组件样式
- 用户端推荐/榜单页新增筛选/排序/空态/无限滚动与卡片栅格对齐
- 前端统一失败弹窗，封装重试逻辑
- 后端新增评论列表 API，支持分页与时间排序
- 评论点赞/互动字段：新增 comment_likes 表与 comment_like_count 字段（或独立统计表），并扩展 DTO

## 架构决策 ADR
### ADR-002: 评论互动数据存储
**上下文:** 评论列表需要互动字段（点赞/互动）以便未来扩展，当前不做热度排序
**决策:** 新增 comment_likes 表记录用户点赞；comments 表增加 like_count 字段用于快速读取
**理由:** 读性能更优且易扩展排序/过滤
**替代方案:** 仅在 comment_likes 表聚合统计 → 拒绝原因: 列表查询性能受限
**影响:** 需要迁移脚本与评论点赞接口

## API设计
### [GET] /api/videos/{id}/comments
- **请求:** page, pageSize, sort=time
- **响应:** 分页结构 { total, page, pageSize, items }
  - 用户端字段: id, content, createdAt, user { id, username }, likeCount, liked
  - 管理端字段: id, content, createdAt, user { id, username, phone, email, status }, likeCount, liked

### [POST] /api/comments/{id}/likes
- **请求:** 无
- **响应:** Void

### [DELETE] /api/comments/{id}/likes
- **请求:** 无
- **响应:** Void

## 数据模型
```sql
ALTER TABLE comments ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE comment_likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_user (comment_id, user_id),
  KEY idx_comment_like_time (comment_id, created_at)
);
```

## 安全与性能
- **安全:** 评论列表遵循最小字段暴露；管理端字段限制在受管接口；鉴权必需
- **性能:** comments.like_count 作为聚合缓存；分页查询使用索引

## 测试与部署
- **测试:** 新增评论列表与点赞接口集成测试，验证分页与字段差异
- **部署:** Flyway 迁移新增字段与表
