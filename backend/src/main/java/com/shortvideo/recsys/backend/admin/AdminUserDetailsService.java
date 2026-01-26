package com.shortvideo.recsys.backend.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "test"})
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminUserMapper adminUserMapper;

    public AdminUserDetailsService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUserEntity admin = adminUserMapper.selectOne(new QueryWrapper<AdminUserEntity>()
                .eq("username", username)
                .last("limit 1"));
        if (admin == null) {
            throw new UsernameNotFoundException("not found");
        }
        boolean disabled = admin.getStatus() != null && admin.getStatus() == 0;
        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .disabled(disabled)
                .authorities(List.of(() -> "ROLE_ADMIN"))
                .build();
    }
}

