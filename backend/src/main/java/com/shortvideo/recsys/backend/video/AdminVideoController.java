package com.shortvideo.recsys.backend.video;

import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.video.dto.AuditRequest;
import com.shortvideo.recsys.backend.video.dto.HotRequest;
import com.shortvideo.recsys.backend.video.dto.VideoDto;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/videos")
@Profile({"docker", "test"})
public class AdminVideoController {
    private final VideoService videoService;

    public AdminVideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ApiResponse<VideoDto> upload(
            @RequestParam("uploaderUserId") long uploaderUserId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tagsJson,
            @RequestPart("video") MultipartFile video
    ) {
        return ApiResponse.ok(videoService.uploadVideo(uploaderUserId, title, description, tagsJson, video));
    }

    @PatchMapping("/{id}/audit")
    public ApiResponse<Void> audit(@PathVariable("id") long id, @org.springframework.web.bind.annotation.RequestBody AuditRequest request) {
        videoService.audit(id, request == null ? null : request.status());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/hot")
    public ApiResponse<Void> hot(@PathVariable("id") long id, @org.springframework.web.bind.annotation.RequestBody HotRequest request) {
        videoService.setHot(id, request != null && Boolean.TRUE.equals(request.isHot()));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") long id) {
        videoService.deleteVideo(id);
        return ApiResponse.ok();
    }
}

