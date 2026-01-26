// 管理端用户服务，封装用户查询与状态变更的业务逻辑。
package com.shortvideo.recsys.backend.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shortvideo.recsys.backend.admin.dto.AdminUserDto;
import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import com.shortvideo.recsys.backend.user.UserEntity;
import com.shortvideo.recsys.backend.user.UserMapper;
import com.shortvideo.recsys.backend.video.dto.PageResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "test"})
public class AdminUserService {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;

    public AdminUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResponse<AdminUserDto> pageUsers(long page, long size, String keyword) {
        long safePage = page <= 0 ? 1 : page;
        long safeSize = size <= 0 ? 20 : Math.min(size, 100);

        QueryWrapper<UserEntity> qw = new QueryWrapper<>();
        String kw = keyword == null ? null : keyword.trim();
        if (kw != null && !kw.isEmpty()) {
            qw.and(w -> w.eq("username", kw)
                    .or().eq("phone", kw)
                    .or().eq("email", kw)
                    .or().like("username", kw)
                    .or().like("phone", kw)
                    .or().like("email", kw));
        }
        qw.orderByDesc("created_at");

        Page<UserEntity> p = userMapper.selectPage(new Page<>(safePage, safeSize), qw);
        List<AdminUserDto> items = p.getRecords().stream()
                .map(this::toDto)
                .toList();
        return new PageResponse<>(p.getTotal(), safePage, safeSize, items);
    }

    public void updateStatus(long id, Integer status) {
        if (id <= 0 || status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCodes.BAD_REQUEST, "参数错误");
        }
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private AdminUserDto toDto(UserEntity user) {
        String createdAt = user.getCreatedAt() == null ? null : DTF.format(user.getCreatedAt());
        return new AdminUserDto(
                user.getId() == null ? 0L : user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus() == null ? 0 : user.getStatus(),
                createdAt
        );
    }
}

