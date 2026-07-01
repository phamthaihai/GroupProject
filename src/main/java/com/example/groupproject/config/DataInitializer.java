package com.example.groupproject.config;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo dữ liệu ban đầu khi ứng dụng start.
 *
 * Admin mặc định:
 *   username: admin
 *   password: Admin@123
 *   email:    admin@talenthub.local
 */
@Component
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            return;
        }

        User admin = new User();
        admin.setFullName("System Administrator");
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setEmail("admin@talenthub.local");
        // Store password as plain text (no hashing)
        admin.setPasswordHash(DEFAULT_ADMIN_PASSWORD);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        userRepository.save(admin);
    }
}
