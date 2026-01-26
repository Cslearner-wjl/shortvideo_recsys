package com.shortvideo.recsys.backend.auth;

import com.shortvideo.recsys.backend.auth.dto.AuthResponse;
import com.shortvideo.recsys.backend.auth.dto.LoginRequest;
import com.shortvideo.recsys.backend.auth.dto.RegisterRequest;
import com.shortvideo.recsys.backend.auth.dto.SendEmailCodeRequest;
import com.shortvideo.recsys.backend.common.ApiResponse;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile({"docker", "test"})
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/email-code")
    public ApiResponse<Void> sendEmailCode(@RequestBody SendEmailCodeRequest request) {
        if (request == null || request.email() == null) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        authService.sendRegisterEmailCode(request.email());
        return ApiResponse.ok();
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
