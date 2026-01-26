// 管理端看板接口控制器，提供统计与热门 TopN 查询能力。
package com.shortvideo.recsys.backend.admin;

import com.shortvideo.recsys.backend.admin.dto.ActiveUserDto;
import com.shortvideo.recsys.backend.admin.dto.DailyPlayDto;
import com.shortvideo.recsys.backend.admin.dto.HotTopnDto;
import com.shortvideo.recsys.backend.admin.dto.UserGrowthDto;
import com.shortvideo.recsys.backend.admin.dto.VideoPublishDto;
import com.shortvideo.recsys.backend.common.ApiResponse;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@Profile({"docker", "test"})
public class AdminAnalyticsController {
    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/daily-play")
    public ApiResponse<List<DailyPlayDto>> dailyPlay(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ApiResponse.ok(adminAnalyticsService.dailyPlay(from, to));
    }

    @GetMapping("/user-growth")
    public ApiResponse<List<UserGrowthDto>> userGrowth(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ApiResponse.ok(adminAnalyticsService.userGrowth(from, to));
    }

    @GetMapping("/hot-topn")
    public ApiResponse<List<HotTopnDto>> hotTopn(
            @RequestParam(value = "n", required = false) Integer n
    ) {
        return ApiResponse.ok(adminAnalyticsService.hotTopn(n));
    }

    @GetMapping("/active-users")
    public ApiResponse<List<ActiveUserDto>> activeUsers(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ApiResponse.ok(adminAnalyticsService.activeUsers(from, to));
    }

    @GetMapping("/video-publish")
    public ApiResponse<List<VideoPublishDto>> videoPublish(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ApiResponse.ok(adminAnalyticsService.videoPublish(from, to));
    }
}
