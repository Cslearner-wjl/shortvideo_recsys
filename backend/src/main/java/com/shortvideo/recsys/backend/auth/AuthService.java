// 鉴权服务，负责注册、登录与用户资料封装。
package com.shortvideo.recsys.backend.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shortvideo.recsys.backend.auth.dto.AuthResponse;
import com.shortvideo.recsys.backend.auth.dto.AuthUserDto;
import com.shortvideo.recsys.backend.auth.dto.LoginRequest;
import com.shortvideo.recsys.backend.auth.dto.RegisterRequest;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "test"})
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom random = new SecureRandom();

    private final UserMapper userMapper;
    private final EmailVerificationCodeMapper emailCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(
            UserMapper userMapper,
            EmailVerificationCodeMapper emailCodeMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.emailCodeMapper = emailCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = Clock.system(ZoneId.of("Asia/Shanghai"));
    }

    public void sendRegisterEmailCode(String email) {
        if (isBlank(email) || !isValidEmail(email)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "邮箱格式不正确");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(clock);

        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setPurpose("REGISTER");
        entity.setExpiresAt(now.plusMinutes(5));
        entity.setUsedAt(null);
        entity.setCreatedAt(now);
        emailCodeMapper.insert(entity);

        log.info("REGISTER email code for {}: {}", email, code);
    }

    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        String username = trim(request.username());
        String phone = trim(request.phone());
        String email = trim(request.email());
        String password = request.password();
        String emailCode = trim(request.emailCode());

        if (isBlank(username) || isBlank(phone) || isBlank(email) || isBlank(password) || isBlank(emailCode)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        if (!isValidPhone(phone)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "手机号必须为11位数字");
        }
        if (!isValidEmail(email)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "邮箱格式不正确");
        }

        if (existsBy("username", username)) {
            throw new BizException(ErrorCodes.USERNAME_EXISTS, "用户名已存在");
        }
        if (existsBy("email", email)) {
            throw new BizException(ErrorCodes.EMAIL_EXISTS, "邮箱已存在");
        }
        if (existsBy("phone", phone)) {
            throw new BizException(ErrorCodes.PHONE_EXISTS, "手机号已存在");
        }

        verifyAndConsumeEmailCode(email, emailCode);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(1);
        userMapper.insert(user);

        String token = jwtService.generateToken(Objects.requireNonNull(user.getId()));
        return new AuthResponse(token, toDto(user));
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        String account = trim(request.account());
        String password = request.password();
        if (isBlank(account) || isBlank(password)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }

        UserEntity user = findByAccount(account);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ErrorCodes.ACCOUNT_OR_PASSWORD_INCORRECT, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCodes.ACCOUNT_FROZEN, "账号已冻结");
        }

        String token = jwtService.generateToken(Objects.requireNonNull(user.getId()));
        return new AuthResponse(token, toDto(user));
    }

    private void verifyAndConsumeEmailCode(String email, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        EmailVerificationCodeEntity latest = emailCodeMapper.selectOne(new QueryWrapper<EmailVerificationCodeEntity>()
                .eq("email", email)
                .eq("purpose", "REGISTER")
                .isNull("used_at")
                .orderByDesc("created_at")
                .last("limit 1"));

        if (latest == null
                || latest.getExpiresAt() == null
                || latest.getExpiresAt().isBefore(now)
                || !Objects.equals(latest.getCode(), code)) {
            throw new BizException(ErrorCodes.EMAIL_CODE_INVALID, "邮箱验证码无效或已过期");
        }

        latest.setUsedAt(now);
        emailCodeMapper.updateById(latest);
    }

    private boolean existsBy(String column, String value) {
        Long count = userMapper.selectCount(new QueryWrapper<UserEntity>().eq(column, value));
        return count != null && count > 0;
    }

    private UserEntity findByAccount(String account) {
        if (isValidEmail(account)) {
            return userMapper.selectOne(new QueryWrapper<UserEntity>().eq("email", account).last("limit 1"));
        }
        if (isValidPhone(account)) {
            return userMapper.selectOne(new QueryWrapper<UserEntity>().eq("phone", account).last("limit 1"));
        }
        return userMapper.selectOne(new QueryWrapper<UserEntity>().eq("username", account).last("limit 1"));
    }

    private static AuthUserDto toDto(UserEntity user) {
        return new AuthUserDto(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getStatus() == null ? 0 : user.getStatus()
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{11}$");
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
