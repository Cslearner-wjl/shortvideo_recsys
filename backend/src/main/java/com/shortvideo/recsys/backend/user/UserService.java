// 用户服务，处理用户资料更新与密码管理。
package com.shortvideo.recsys.backend.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.dto.ChangePasswordRequest;
import com.shortvideo.recsys.backend.user.dto.UpdateProfileRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 用户服务 - 提供用户信息修改能力
 */
@Service
@Profile({"docker", "test"})
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 更新用户个人信息
     */
    @Transactional
    public void updateProfile(long userId, UpdateProfileRequest request) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "用户不存在");
        }

        boolean changed = false;

        // 更新用户名
        String username = trim(request.username());
        if (username != null && !username.isEmpty() && !Objects.equals(username, user.getUsername())) {
            if (username.length() < 2 || username.length() > 32) {
                throw new BizException(ErrorCodes.BAD_REQUEST, "用户名长度需在2-32字符之间");
            }
            // 检查唯一性
            UserEntity existing = userMapper.selectOne(new QueryWrapper<UserEntity>()
                    .eq("username", username)
                    .ne("id", userId)
                    .last("limit 1"));
            if (existing != null) {
                throw new BizException(ErrorCodes.USERNAME_EXISTS, "用户名已存在");
            }
            user.setUsername(username);
            changed = true;
        }

        // 更新手机号
        String phone = trim(request.phone());
        if (phone != null && !phone.isEmpty() && !Objects.equals(phone, user.getPhone())) {
            if (!phone.matches("^1\\d{10}$")) {
                throw new BizException(ErrorCodes.BAD_REQUEST, "手机号格式不正确");
            }
            UserEntity existing = userMapper.selectOne(new QueryWrapper<UserEntity>()
                    .eq("phone", phone)
                    .ne("id", userId)
                    .last("limit 1"));
            if (existing != null) {
                throw new BizException(ErrorCodes.PHONE_EXISTS, "手机号已被使用");
            }
            user.setPhone(phone);
            changed = true;
        }

        // 更新邮箱
        String email = trim(request.email());
        if (email != null && !email.isEmpty() && !Objects.equals(email, user.getEmail())) {
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BizException(ErrorCodes.BAD_REQUEST, "邮箱格式不正确");
            }
            UserEntity existing = userMapper.selectOne(new QueryWrapper<UserEntity>()
                    .eq("email", email)
                    .ne("id", userId)
                    .last("limit 1"));
            if (existing != null) {
                throw new BizException(ErrorCodes.EMAIL_EXISTS, "邮箱已被使用");
            }
            user.setEmail(email);
            changed = true;
        }

        // 更新头像
        String avatarUrl = trim(request.avatarUrl());
        if (avatarUrl != null && !avatarUrl.isEmpty() && !Objects.equals(avatarUrl, user.getAvatarUrl())) {
            if (avatarUrl.length() > 512) {
                throw new BizException(ErrorCodes.BAD_REQUEST, "头像地址过长");
            }
            user.setAvatarUrl(avatarUrl);
            changed = true;
        }

        // 更新简介
        String bio = trim(request.bio());
        if (bio != null && !bio.isEmpty() && !Objects.equals(bio, user.getBio())) {
            if (bio.length() > 255) {
                throw new BizException(ErrorCodes.BAD_REQUEST, "简介长度不能超过255字符");
            }
            user.setBio(bio);
            changed = true;
        }

        if (changed) {
            userMapper.updateById(user);
        }
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(long userId, ChangePasswordRequest request) {
        if (request == null || request.oldPassword() == null || request.newPassword() == null) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCodes.ACCOUNT_OR_PASSWORD_INCORRECT, "原密码错误");
        }

        // 验证新密码格式
        String newPassword = request.newPassword();
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "新密码长度需在6-32字符之间");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
