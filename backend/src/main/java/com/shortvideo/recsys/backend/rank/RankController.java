package com.shortvideo.recsys.backend.rank;

import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.rank.dto.HotRankVideoDto;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rank")
@Profile({"docker", "test"})
public class RankController {
    private final HotRankService hotRankService;

    public RankController(HotRankService hotRankService) {
        this.hotRankService = hotRankService;
    }

    @GetMapping("/hot")
    public ApiResponse<PageResponse<HotRankVideoDto>> hot(
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") long pageSize
    ) {
        return ApiResponse.ok(hotRankService.page(page, pageSize));
    }
}

