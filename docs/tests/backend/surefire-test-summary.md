# Backend Surefire 测试总结

- 统计: 21 个测试类 / 32 个用例，失败 0、错误 0、跳过 1，总耗时 13.572s（来源: backend/target/surefire-reports）
- 覆盖范围: 应用启动、健康检查、管理后台、认证/权限校验、推荐与标签解析、热榜与热度计算、用户资料更新、评论/点赞流程、互动异常、视频存储

## 详细用例清单
- 应用启动测试（BackendApplicationTests，1 个用例，0.603s）：contextLoads
- 健康检查接口测试（HealthControllerTest，1 个用例，0.581s）：health_shouldReturnOk
- 管理后台-数据统计集成测试（AdminAnalyticsIntegrationTest，1 个用例，0.460s）：analyticsEndpoints_shouldReturnAggregatedData
- 管理后台-用户管理集成测试（AdminUserIntegrationTest，3 个用例，0.757s）：listUsers_shouldSupportKeywordSearch、adminAccounts_shouldSupportCrud、updateStatus_shouldFreezeUser
- 认证流程集成测试（AuthFlowIntegrationTest，2 个用例，0.166s）：login_wrongPassword_shouldReturnGenericError、register_login_me_shouldWork
- 鉴权参数校验集成测试（AuthValidationIntegrationTest，3 个用例，0.031s）：sendEmailCode_invalidEmail_shouldReturnBadRequest、register_invalidPhone_shouldReturnBadRequest、login_emptyAccount_shouldReturnBadRequest
- 冻结用户集成测试（FrozenUserIntegrationTest，1 个用例，0.169s）：frozenUser_shouldNotLogin
- 访问控制集成测试（AccessControlIntegrationTest，2 个用例，0.031s）：unauthenticated_userEndpoints_shouldReturnUnauthorized、adminEndpoint_withoutBasic_shouldReturnUnauthorized
- 推荐主流程集成测试（RecommendationIntegrationTest，2 个用例，0.408s）：existingUser_shouldPreferTags_andDedupSeenVideos、newUser_shouldReturnSomeItems
- 推荐边界集成测试（RecommendationBoundaryIntegrationTest，2 个用例，0.258s）：cursor_invalid_shouldFallbackToFirstPage、pageSize_shouldClampToMax
- ALS 推荐兜底（Redis 缺失）集成测试（RecommendationAlsFallbackIntegrationTest，1 个用例，0.958s）：alsEnabled_butRedisMissing_shouldFallbackToRuleRecommendation
- ALS 推荐兜底（缓存无效）集成测试（RecommendationAlsInvalidCacheFallbackIntegrationTest，1 个用例，2.241s）：alsEnabled_butRedisHasInvalidIds_shouldFallbackToRuleRecommendation
- 视频标签解析单元测试（VideoTagsParserTest，1 个用例，0.004s）：parseTags_shouldHandleJsonArrayAndCsv
- 热榜刷新与排序集成测试（HotRankIntegrationTest，1 个用例，0.121s）：refresh_thenRankHot_shouldReturnByHotScoreDesc
- 热榜边界集成测试（HotRankBoundaryIntegrationTest，2 个用例，0.063s）：page_shouldFallbackToOne_whenNonPositive、pageSize_shouldClampToMax
- 热度分数计算单元测试（HotScoreCalculatorTest，1 个用例，0.003s）：compute_shouldUseLog1pAndWeights
- 用户资料更新集成测试（UserProfileIntegrationTest，1 个用例，5.840s）：updateProfile_shouldUpdateUserInfo
- 评论/点赞流转集成测试（CommentFlowIntegrationTest，1 个用例，0.158s）：comment_like_shouldUpdateList
- 互动异常与幂等集成测试（VideoInteractionEdgeIntegrationTest，3 个用例，0.595s）：duplicateLike_shouldBeIdempotent、emptyComment_shouldReturnBadRequest、unapprovedVideo_like_shouldReturnNotFound
- 点赞流程集成测试（VideoLikeFlowIntegrationTest，1 个用例，0.125s）：login_like_shouldUpdateCount
- 视频存储链路集成测试（VideoMinioIntegrationTest，1 个用例，0.000s，跳过 1）：upload_audit_feed_delete_shouldWork
