package com.shortvideo.recsys.backend.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"docker", "test"})
public class AdminUserBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminUserBootstrap.class);

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.enabled:true}")
    private boolean enabled;

    @Value("${app.admin.bootstrap.username:admin}")
    private String username;

    @Value("${app.admin.bootstrap.password:AdminPass123}")
    private String password;

    @Value("${app.admin.bootstrap.display-name:Admin}")
    private String displayName;

    @Value("${app.admin.bootstrap.reset-password:false}")
    private boolean resetPassword;

    public AdminUserBootstrap(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        try {
            AdminUserEntity existing = adminUserMapper.selectOne(new QueryWrapper<AdminUserEntity>()
                    .eq("username", username)
                    .last("limit 1"));
            if (existing != null) {
                if (!resetPassword) {
                    return;
                }

                existing.setDisplayName(displayName);
                existing.setPasswordHash(passwordEncoder.encode(password));
                existing.setStatus(1);
                adminUserMapper.updateById(existing);
                log.warn("Bootstrap admin user password reset: username='{}' (profile docker/test). Disable via app.admin.bootstrap.reset-password=false", username);
                return;
            }

            AdminUserEntity admin = new AdminUserEntity();
            admin.setUsername(username);
            admin.setDisplayName(displayName);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setStatus(1);
            adminUserMapper.insert(admin);

            log.warn("Bootstrap admin user created: username='{}' (profile docker/test). Change it via app.admin.bootstrap.* or disable via app.admin.bootstrap.enabled=false", username);
        } catch (Exception e) {
            // Avoid failing app startup for bootstrap convenience.
            log.warn("Skip admin bootstrap due to error: {}", e.getMessage());
        }
    }
}
