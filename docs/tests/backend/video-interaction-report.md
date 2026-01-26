# 互动异常与幂等测试报告

## 1. 测试目的
验证互动行为在重复操作、资源状态异常与非法参数场景下的幂等与错误处理是否正确。

## 2. 测试范围
- 点赞幂等与行为记录：`/api/videos/{id}/like`
- 未审核资源交互限制：`/api/videos/{id}/like`
- 评论内容校验：`/api/videos/{id}/comments`

## 3. 测试环境
- Profile: test
- 数据库: H2 内存库（schema-h2.sql）
- 框架: Spring Boot Test + MockMvc
- 时间: 2026-01-22

## 4. 用例清单与断言
- VideoInteractionEdgeIntegrationTest
  - duplicateLike_shouldBeIdempotent
    - 断言：重复点赞后 like_count=1，user_actions 仅一条 LIKE
  - unapprovedVideo_like_shouldReturnNotFound
    - 断言：未审核视频点赞返回 HTTP 404，code=40401
  - emptyComment_shouldReturnBadRequest
    - 断言：空评论返回 HTTP 400，code=40000，message=评论内容不能为空

## 5. 执行结果
- 统计：3 用例，失败 0，错误 0，跳过 0，用时 0.595s
- 详情：VideoInteractionEdgeIntegrationTest（3 用例，0.595s）

## 6. 结论
互动异常与幂等场景验证通过，计数与错误码符合预期。
