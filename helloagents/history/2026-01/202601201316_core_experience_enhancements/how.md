# 技术设计: 核心功能完善与体验增强

## 技术方案
### 核心技术
- 后端: Spring Boot 3.2.2 + MyBatis-Plus
- 前端: Vue 3 + Vite + Element Plus + ECharts

### 实现要点
- 后端补齐用户信息修改、管理员账号管理与统计接口
- 前端新增用户资料页、视频详情页、管理端统计图表与管理员管理页面
- 复用现有评论与互动接口，补齐列表展示与交互状态

## API设计
### PATCH /api/users/me
- **请求:** {"nickname": "...", "avatarUrl": "...", "bio": "..."}
- **响应:** {"code":0,"data":{"id":...,"nickname":"...",...}}

### GET /api/videos/{id}/comments
- **请求:** page/size 参数
- **响应:** 评论分页列表（含用户、时间、是否点赞）

### /api/admin/admins
- **GET:** 管理员列表
- **POST:** 新增管理员
- **PATCH:** 修改管理员
- **PATCH /{id}/password:** 修改管理员密码
- **DELETE:** 删除管理员

### GET /api/admin/analytics/video-publish
- **请求:** from/to
- **响应:** 时间序列数据

## 数据模型
```sql
-- 复用现有表，新增必要字段与索引（如需）
```

## 安全与性能
- **安全:** 管理端接口继续使用 Basic Auth 与角色校验；用户修改仅允许本人
- **性能:** 列表接口分页；统计接口按时间范围聚合

## 测试与部署
- **测试:** 后端新增集成测试覆盖用户修改、管理员管理、统计接口
- **部署:** 无新增基础设施
