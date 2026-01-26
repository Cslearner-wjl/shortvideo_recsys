# 任务清单: 后端补测与报告完善

目录: `helloagents/plan/202601221426_backend-test-coverage/`


## 1. 权限与参数校验补测
- [√] 1.1 新增 `backend/src/test/java/com/shortvideo/recsys/backend/auth/AuthValidationIntegrationTest.java` 覆盖非法邮箱/手机号/空参数登录注册，验证 why.md#需求-权限与参数校验补测-场景-非法参数触发校验
- [√] 1.2 新增 `backend/src/test/java/com/shortvideo/recsys/backend/common/AccessControlIntegrationTest.java` 覆盖未登录访问 `/api/users/me`、`/api/recommendations` 与未携带 Basic 访问 `/api/admin/users`，验证 why.md#需求-权限与参数校验补测-场景-未登录访问受限接口

## 2. 推荐与热榜边界补测
- [√] 2.1 新增 `backend/src/test/java/com/shortvideo/recsys/backend/recommendation/RecommendationBoundaryIntegrationTest.java` 覆盖 pageSize 超限与 cursor 非法输入，验证 why.md#需求-推荐与热榜边界补测-场景-pageSize-超限 与 why.md#需求-推荐与热榜边界补测-场景-cursor-非法
- [√] 2.2 新增 `backend/src/test/java/com/shortvideo/recsys/backend/rank/HotRankBoundaryIntegrationTest.java` 覆盖 page/pageSize 边界与空榜场景，验证 why.md#需求-推荐与热榜边界补测-场景-pageSize-超限

## 3. 互动异常与幂等补测
- [√] 3.1 新增 `backend/src/test/java/com/shortvideo/recsys/backend/video/VideoInteractionEdgeIntegrationTest.java` 覆盖重复点赞幂等、未审核资源交互返回 404、空评论参数校验，验证 why.md#需求-互动异常与幂等补测-场景-重复点赞取消 与 why.md#需求-互动异常与幂等补测-场景-资源未通过审核

## 4. 安全检查
- [√] 4.1 执行安全检查（输入校验、权限控制、敏感信息处理、EHRB风险规避）

## 5. 文档更新
- [√] 5.1 在 `docs/test/backend/permissions-validation-report.md` 记录权限与参数校验测试报告
- [√] 5.2 在 `docs/test/backend/recommendation-rank-report.md` 记录推荐/热榜边界测试报告
- [√] 5.3 在 `docs/test/backend/video-interaction-report.md` 记录互动异常与幂等测试报告

## 6. 测试
- [√] 6.1 执行后端测试：`mvn test`（backend 目录）
- [√] 6.2 更新三类测试报告中的执行结果与统计摘要
