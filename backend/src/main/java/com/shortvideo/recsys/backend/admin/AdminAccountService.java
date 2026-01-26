package com.shortvideo.recsys.backend.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortvideo.recsys.backend.admin.dto.AdminAccountDto;
import com.shortvideo.recsys.backend.admin.dto.AdminAccountRequest;
import com.shortvideo.recsys.backend.admin.dto.AdminChangePasswordRequest;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理员账号管理服务
 */
@Service
@Profile({"docker", "test"})
public class AdminAccountService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 分页查询管理员列表
     */
    public PageResponse<AdminAccountDto> pageAdmins(long page, long size, String keyword) {
        long safePage = page <= 0 ? 1 : page;
        long safeSize = size <= 0 ? 20 : Math.min(size, 100);

        QueryWrapper<AdminUserEntity> qw = new QueryWrapper<>();
        String kw = keyword == null ? null : keyword.trim();
        if (kw != null && !kw.isEmpty()) {
            qw.like("username", kw);
        }
        qw.orderByDesc("created_at");

        Page<AdminUserEntity> p = adminUserMapper.selectPage(new Page<>(safePage, safeSize), qw);
        List<AdminAccountDto> items = p.getRecords().stream()
                .map(this::toDto)
                .toList();
        return new PageResponse<>(p.getTotal(), safePage, safeSize, items);
    }

    /**
     * 创建管理员
     */
    @Transactional
    public AdminAccountDto createAdmin(AdminAccountRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "用户名和密码不能为空");
        }

        String username = request.username().trim();
        String password = request.password();

        if (username.length() < 2 || username.length() > 32) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "用户名长度需在2-32字符之间");
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "密码长度需在6-32字符之间");
        }

        // 检查用户名是否已存在
        AdminUserEntity existing = adminUserMapper.selectOne(
                new QueryWrapper<AdminUserEntity>().eq("username", username).last("limit 1"));
        if (existing != null) {
            throw new BizException(ErrorCodes.USERNAME_EXISTS, "用户名已存在");
        }

        AdminUserEntity entity = new AdminUserEntity();
        entity.setUsername(username);
        entity.setPasswordHash(passwordEncoder.encode(password));
        adminUserMapper.insert(entity);

        return toDto(entity);
    }

    /**
     * 更新管理员信息
     */
    @Transactional
    public AdminAccountDto updateAdmin(long id, AdminAccountRequest request) {
        AdminUserEntity admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "管理员不存在");
        }

        boolean changed = false;

        // 更新用户名
        if (request != null && !isBlank(request.username())) {
            String username = request.username().trim();
            if (!username.equals(admin.getUsername())) {
                if (username.length() < 2 || username.length() > 32) {
                    throw new BizException(ErrorCodes.BAD_REQUEST, "用户名长度需在2-32字符之间");
                }
                AdminUserEntity existing = adminUserMapper.selectOne(
                        new QueryWrapper<AdminUserEntity>()
                                .eq("username", username)
                                .ne("id", id)
                                .last("limit 1"));
                if (existing != null) {
                    throw new BizException(ErrorCodes.USERNAME_EXISTS, "用户名已存在");
                }
                admin.setUsername(username);
                changed = true;
            }
        }

        // 更新密码（如果提供）
        if (request != null && !isBlank(request.password())) {
            String password = request.password();
            if (password.length() < 6 || password.length() > 32) {
                throw new BizException(ErrorCodes.BAD_REQUEST, "密码长度需在6-32字符之间");
            }
            admin.setPasswordHash(passwordEncoder.encode(password));
            changed = true;
        }

        if (changed) {
            adminUserMapper.updateById(admin);
        }

        return toDto(admin);
    }

    /**
     * 删除管理员
     */
    @Transactional
    public void deleteAdmin(long id) {
        AdminUserEntity admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "管理员不存在");
        }

        // 不允许删除最后一个管理员
        Long count = adminUserMapper.selectCount(new QueryWrapper<>());
        if (count != null && count <= 1) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "不能删除最后一个管理员");
        }

        adminUserMapper.deleteById(id);
    }

    /**
     * 管理员修改自己的密码
     */
    @Transactional
    public void changePassword(String username, AdminChangePasswordRequest request) {
        if (request == null || isBlank(request.oldPassword()) || isBlank(request.newPassword())) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }

        AdminUserEntity admin = adminUserMapper.selectOne(
                new QueryWrapper<AdminUserEntity>().eq("username", username).last("limit 1"));
        if (admin == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "管理员不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.oldPassword(), admin.getPasswordHash())) {
            throw new BizException(ErrorCodes.ACCOUNT_OR_PASSWORD_INCORRECT, "原密码错误");
        }

        // 验证新密码格式
        String newPassword = request.newPassword();
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "新密码长度需在6-32字符之间");
        }

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserMapper.updateById(admin);
    }

    private AdminAccountDto toDto(AdminUserEntity entity) {
        String createdAt = entity.getCreatedAt() == null ? null : DTF.format(entity.getCreatedAt());
        return new AdminAccountDto(
                entity.getId() == null ? 0L : entity.getId(),
                entity.getUsername(),
                createdAt
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
