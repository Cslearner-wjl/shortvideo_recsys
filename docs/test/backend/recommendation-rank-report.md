# 推荐与热榜边界测试报告

## 1. 测试目的
验证推荐与热榜接口在分页参数与游标异常输入场景下的边界处理是否符合预期。

## 2. 测试范围
- 推荐分页与游标：`/api/recommendations`
- 热榜分页：`/api/rank/hot`

## 3. 测试环境
- Profile: test
- 数据库: H2 内存库（schema-h2.sql）
- 框架: Spring Boot Test + MockMvc
- 时间: 2026-01-22

## 4. 用例清单与断言
- RecommendationBoundaryIntegrationTest
  - pageSize_shouldClampToMax
    - 断言：pageSize 超限后响应 pageSize=100
  - cursor_invalid_shouldFallbackToFirstPage
    - 断言：cursor 非法时响应 page=1、pageSize=10
- HotRankBoundaryIntegrationTest
  - pageSize_shouldClampToMax
    - 断言：pageSize 超限后响应 pageSize=100
  - page_shouldFallbackToOne_whenNonPositive
    - 断言：page<=0 时响应 page=1、pageSize=20

## 5. 执行结果
- 统计：4 用例，失败 0，错误 0，跳过 0，用时 0.321s
- 详情：
  - RecommendationBoundaryIntegrationTest（2 用例，0.258s）
  - HotRankBoundaryIntegrationTest（2 用例，0.063s）

## 6. 结论
推荐与热榜接口对分页与游标边界处理符合预期，未发现异常回归。
