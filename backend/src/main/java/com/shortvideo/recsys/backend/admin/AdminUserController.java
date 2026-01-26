// 管理端用户接口控制器，提供分页查询与冻结/解封能力。
package com.shortvideo.recsys.backend.admin;

import com.shortvideo.recsys.backend.admin.dto.AdminUserDto;
import com.shortvideo.recsys.backend.admin.dto.AdminUserStatusRequest;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Profile({"docker", "test"})
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserDto>> page(
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "size", required = false, defaultValue = "20") long size,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(adminUserService.pageUsers(page, size, keyword));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable("id") long id,
            @RequestBody(required = false) AdminUserStatusRequest request
    ) {
        adminUserService.updateStatus(id, request == null ? null : request.status());
        return ApiResponse.ok();
    }
}

