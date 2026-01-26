package com.shortvideo.recsys.backend.video;

import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.video.dto.AdminCommentItemDto;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/videos")
@Profile({"docker", "test"})
public class AdminCommentController {
    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<PageResponse<AdminCommentItemDto>> list(
            @PathVariable("id") long videoId,
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") long pageSize,
            @RequestParam(value = "sort", required = false, defaultValue = "time") String sort
    ) {
        return ApiResponse.ok(commentService.pageForAdmin(videoId, page, pageSize, sort));
    }
}
