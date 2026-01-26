// 用户端资料与账户接口，提供查询与更新能力。
package com.shortvideo.recsys.backend.user;

import com.shortvideo.recsys.backend.auth.AuthenticatedUser;
import com.shortvideo.recsys.backend.auth.dto.AuthUserDto;
import com.shortvideo.recsys.backend.auth.dto.MeResponse;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.dto.ChangePasswordRequest;
import com.shortvideo.recsys.backend.user.dto.UpdateProfileRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Profile({"docker", "test"})
public class UserController {
    private final UserMapper userMapper;
    private final UserService userService;

    public UserController(UserMapper userMapper, UserService userService) {
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
        UserEntity user = userMapper.selectById(principal.userId());
        if (user == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
        AuthUserDto dto = new AuthUserDto(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getStatus() == null ? 0 : user.getStatus()
        );
        return ApiResponse.ok(new MeResponse(dto));
    }

    /**
     * 更新当前用户个人信息
     */
    @PutMapping("/me")
    public ApiResponse<MeResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody UpdateProfileRequest request
    ) {
        long userId = requireUserId(principal);
        userService.updateProfile(userId, request);

        // 返回更新后的用户信息
        UserEntity user = userMapper.selectById(userId);
        AuthUserDto dto = new AuthUserDto(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getStatus() == null ? 0 : user.getStatus()
        );
        return ApiResponse.ok(new MeResponse(dto));
    }

    /**
     * 修改当前用户密码
     */
    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody ChangePasswordRequest request
    ) {
        long userId = requireUserId(principal);
        userService.changePassword(userId, request);
        return ApiResponse.ok();
    }

    private static long requireUserId(AuthenticatedUser principal) {
        if (principal == null || principal.userId() <= 0) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }
        return principal.userId();
    }
}
