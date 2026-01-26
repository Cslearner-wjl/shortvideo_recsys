package com.shortvideo.recsys.backend.video;

import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.auth.AuthenticatedUser;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import com.shortvideo.recsys.backend.video.dto.VideoDto;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@Profile({"docker", "test"})
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoDto> getById(
            @PathVariable("id") long id,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        Long viewerId = principal == null ? null : principal.userId();
        return ApiResponse.ok(videoService.getApprovedVideo(id, viewerId));
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<VideoDto>> page(
            @RequestParam(value = "sort", required = false, defaultValue = "time") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") long pageSize,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        Long viewerId = principal == null ? null : principal.userId();
        return ApiResponse.ok(videoService.pageApproved(sort, page, pageSize, viewerId));
    }
}
