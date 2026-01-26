package com.shortvideo.recsys.backend.video;

import com.shortvideo.recsys.backend.auth.AuthenticatedUser;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.video.dto.CommentItemDto;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Profile({"docker", "test"})
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/videos/{id}/comments")
    public ApiResponse<PageResponse<CommentItemDto>> list(
            @PathVariable("id") long videoId,
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") long pageSize,
            @RequestParam(value = "sort", required = false, defaultValue = "time") String sort,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        Long viewerId = principal == null ? null : principal.userId();
        return ApiResponse.ok(commentService.pageForUser(videoId, page, pageSize, sort, viewerId));
    }

    @PostMapping("/comments/{id}/likes")
    public ApiResponse<Void> like(
            @PathVariable("id") long commentId,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        commentService.like(commentId, requireUserId(principal));
        return ApiResponse.ok();
    }

    @DeleteMapping("/comments/{id}/likes")
    public ApiResponse<Void> unlike(
            @PathVariable("id") long commentId,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        commentService.unlike(commentId, requireUserId(principal));
        return ApiResponse.ok();
    }

    private static long requireUserId(AuthenticatedUser principal) {
        if (principal == null || principal.userId() <= 0) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
        return principal.userId();
    }
}
