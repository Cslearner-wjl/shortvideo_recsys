package com.shortvideo.recsys.backend.rank;

import com.shortvideo.recsys.backend.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rank")
@Profile({"docker", "test"})
public class AdminRankController {
    private final HotRankService hotRankService;

    public AdminRankController(HotRankService hotRankService) {
        this.hotRankService = hotRankService;
    }

    @PostMapping("/hot/refresh")
    public ApiResponse<Void> refreshHot() {
        hotRankService.refresh();
        return ApiResponse.ok(null);
    }
}

