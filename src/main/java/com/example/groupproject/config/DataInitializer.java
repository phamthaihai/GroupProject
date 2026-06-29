package com.example.groupproject.config;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo dữ liệu ban đầu khi ứng dụng start.
 *
 * Chỉ chạy nếu chưa có ADMIN nào trong database.
 * @Profile("!test") đảm bảo không chạy trong test context.
 *
 * Admin mặc định:
 *   username: admin
 *   password: Admin@123 (BCrypt encoded)
 *   email:    admin@talenthub.local
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            return; // đã có admin, không tạo thêm
        }

        User admin = new User();
        admin.setFullName("System Administrator");
        admin.setUsername("admin");
        admin.setEmail("admin@talenthub.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        // createdAt và updatedAt sẽ được set bởi @PrePersist trong User entity

        userRepository.save(admin);
    }
}
