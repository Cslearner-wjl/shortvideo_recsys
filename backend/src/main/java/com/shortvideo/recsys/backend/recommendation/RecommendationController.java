package com.shortvideo.recsys.backend.recommendation;

import com.shortvideo.recsys.backend.auth.AuthenticatedUser;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.recommendation.dto.RecommendationResponse;
import com.shortvideo.recsys.backend.recommendation.dto.RecommendationVideoDto;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@Profile({"docker", "test"})
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ApiResponse<RecommendationResponse<RecommendationVideoDto>> recommend(
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") long pageSize,
            @RequestParam(value = "cursor", required = false) String cursor,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        long userId = requireUserId(principal);
        return ApiResponse.ok(recommendationService.recommend(userId, page, pageSize, cursor));
    }

    private static long requireUserId(AuthenticatedUser principal) {
        if (principal == null || principal.userId() <= 0) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
        return principal.userId();
    }
}

