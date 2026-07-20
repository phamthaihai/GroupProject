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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
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
        admin.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        userRepository.save(admin);
        // ... code tạo admin ở trên ...
        userRepository.save(admin);

        // --- THÊM ĐOẠN NÀY ĐỂ TẠO ỨNG VIÊN TEST ---
        User candidate = new User();
        candidate.setFullName("Ứng viên Test");
        candidate.setUsername("testuser");
        candidate.setEmail("test@gmail.com");
        candidate.setPasswordHash("abc"); // Dùng setPassword theo tên cột trong DB của bạn
        candidate.setRole(UserRole.CANDIDATE);
        candidate.setStatus(UserStatus.ACTIVE);
        candidate.setFailedLoginCount((short) 0); // Bắt buộc vì DB yêu cầu not-null
        candidate.setEmailVerified(true); // Bắt buộc vì DB yêu cầu not-null
        userRepository.save(candidate);
        // ------------------------------------------
    }
}
