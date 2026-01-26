package com.shortvideo.recsys.backend.video;

import com.shortvideo.recsys.backend.auth.AuthenticatedUser;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.video.dto.CreateCommentRequest;
import com.shortvideo.recsys.backend.video.dto.PlayRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@Profile({"docker", "test"})
public class VideoInteractionController {
    private final VideoInteractionService videoInteractionService;

    public VideoInteractionController(VideoInteractionService videoInteractionService) {
        this.videoInteractionService = videoInteractionService;
    }

    @PostMapping("/{id}/play")
    public ApiResponse<Void> play(
            @PathVariable("id") long videoId,
            @RequestBody(required = false) PlayRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        videoInteractionService.play(
                videoId,
                requireUserId(principal),
                request == null ? null : request.durationMs(),
                request == null ? null : request.isCompleted()
        );
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@PathVariable("id") long videoId, @AuthenticationPrincipal AuthenticatedUser principal) {
        videoInteractionService.like(videoId, requireUserId(principal));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<Void> unlike(@PathVariable("id") long videoId, @AuthenticationPrincipal AuthenticatedUser principal) {
        videoInteractionService.unlike(videoId, requireUserId(principal));
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> favorite(@PathVariable("id") long videoId, @AuthenticationPrincipal AuthenticatedUser principal) {
        videoInteractionService.favorite(videoId, requireUserId(principal));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable("id") long videoId, @AuthenticationPrincipal AuthenticatedUser principal) {
        videoInteractionService.unfavorite(videoId, requireUserId(principal));
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<Void> comment(
            @PathVariable("id") long videoId,
            @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        videoInteractionService.comment(videoId, requireUserId(principal), request == null ? null : request.content());
        return ApiResponse.ok();
    }

    private static long requireUserId(AuthenticatedUser principal) {
        if (principal == null || principal.userId() <= 0) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
        return principal.userId();
    }
}
