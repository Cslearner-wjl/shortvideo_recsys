package com.shortvideo.recsys.backend.auth;

import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"docker", "test"})
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public JwtAuthFilter(JwtService jwtService, UserMapper userMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = Optional.ofNullable(request.getRequestURI()).orElse("");
        return path.startsWith("/api/auth/")
                || path.startsWith("/actuator/")
                || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();
        long userId = jwtService.verifyAndGetUserId(token);

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或Token无效");
        }

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getStatus());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (principal.isFrozen() && isWriteMethod(request.getMethod())) {
            throw new BizException(ErrorCodes.ACCOUNT_FROZEN, "账号已冻结");
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }
}
