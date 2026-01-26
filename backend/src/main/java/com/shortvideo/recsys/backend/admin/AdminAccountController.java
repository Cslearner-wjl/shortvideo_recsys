package com.shortvideo.recsys.backend.admin;

import com.shortvideo.recsys.backend.admin.dto.AdminAccountDto;
import com.shortvideo.recsys.backend.admin.dto.AdminAccountRequest;
import com.shortvideo.recsys.backend.admin.dto.AdminChangePasswordRequest;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员账号管理控制器
 */
@RestController
@RequestMapping("/api/admin/admins")
@Profile({"docker", "test"})
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    /**
     * 分页查询管理员列表
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminAccountDto>> page(
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "size", required = false, defaultValue = "20") long size,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(adminAccountService.pageAdmins(page, size, keyword));
    }

    /**
     * 创建管理员
     */
    @PostMapping
    public ApiResponse<AdminAccountDto> create(@RequestBody AdminAccountRequest request) {
        return ApiResponse.ok(adminAccountService.createAdmin(request));
    }

    /**
     * 更新管理员
     */
    @PutMapping("/{id}")
    public ApiResponse<AdminAccountDto> update(
            @PathVariable("id") long id,
            @RequestBody AdminAccountRequest request
    ) {
        return ApiResponse.ok(adminAccountService.updateAdmin(id, request));
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") long id) {
        adminAccountService.deleteAdmin(id);
        return ApiResponse.ok();
    }

    /**
     * 当前管理员修改密码
     */
    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(@RequestBody AdminChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        adminAccountService.changePassword(username, request);
        return ApiResponse.ok();
    }
}
