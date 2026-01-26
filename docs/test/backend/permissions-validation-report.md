# 权限与参数校验测试报告

## 1. 测试目的
验证后端在未授权访问与非法参数输入场景下的错误码、状态码与提示信息是否符合预期。

## 2. 测试范围
- 未登录访问：`/api/users/me`、`/api/recommendations`
- 管理端未携带 Basic：`/api/admin/users`
- 鉴权参数校验：邮箱验证码、注册手机号、登录空参数

## 3. 测试环境
- Profile: test
- 数据库: H2 内存库（schema-h2.sql）
- 框架: Spring Boot Test + MockMvc
- 时间: 2026-01-22

## 4. 用例清单与断言
- AccessControlIntegrationTest
  - unauthenticated_userEndpoints_shouldReturnUnauthorized
    - 断言：HTTP 401，code=40100，message=未登录或Token无效
  - adminEndpoint_withoutBasic_shouldReturnUnauthorized
    - 断言：HTTP 401
- AuthValidationIntegrationTest
  - sendEmailCode_invalidEmail_shouldReturnBadRequest
    - 断言：HTTP 400，code=40000，message=邮箱格式不正确
  - register_invalidPhone_shouldReturnBadRequest
    - 断言：HTTP 400，code=40000，message=手机号必须为11位数字
  - login_emptyAccount_shouldReturnBadRequest
    - 断言：HTTP 400，code=40000，message=参数错误

## 5. 执行结果
- 统计：5 用例，失败 0，错误 0，跳过 0，用时 0.062s
- 详情：
  - AccessControlIntegrationTest（2 用例，0.031s）
  - AuthValidationIntegrationTest（3 用例，0.031s）

## 6. 结论
权限与参数校验覆盖通过，返回码与提示符合约定，无阻断问题。
