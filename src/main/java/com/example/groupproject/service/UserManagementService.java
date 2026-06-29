package com.example.groupproject.service;

import com.example.groupproject.dto.CreateUserForm;
import com.example.groupproject.entity.ActivityLog;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.ActivityEventType;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.ActivityLogRepository;
import com.example.groupproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý vòng đời tài khoản người dùng (Admin use cases).
 *
 * Các thao tác:
 *   - searchUsers: tìm kiếm user với bộ lọc
 *   - createUser: tạo HR_MANAGER hoặc INTERVIEWER account
 *   - unlockUser: mở khóa tài khoản bị LOCKED
 *   - deactivateUser: deactivate tài khoản (ACTIVE/LOCKED → INACTIVE)
 *   - canDeactivate/canUnlock: kiểm tra quyền để render action buttons trên UI
 *
 * Tất cả thao tác ghi đều log vào activity_log.
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    /** Chỉ ADMIN được tạo HR_MANAGER và INTERVIEWER (không tạo ADMIN/CANDIDATE qua UI) */
    private static final Set<UserRole> CREATABLE_ROLES = Set.of(UserRole.HR_MANAGER, UserRole.INTERVIEWER);

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> searchUsers(String search, UserRole role, UserStatus status) {
        return userRepository.searchUsers(
                search != null ? search.trim() : null,
                role,
                status
        );
    }

    /**
     * Tạo user mới với role HR_MANAGER hoặc INTERVIEWER.
     * Throws IllegalArgumentException nếu role không hợp lệ, hoặc username/email đã tồn tại.
     */
    @Transactional
    public User createUser(CreateUserForm form, User actor) {
        UserRole role = form.getUserRole();
        if (!CREATABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Only HR Manager or Interviewer accounts can be created");
        }
        if (userRepository.findByUsername(form.getUsername()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(form.getEmail()) != null) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        // createdAt/updatedAt sẽ được set bởi @PrePersist trong User entity

        User saved = userRepository.save(user);
        logActivity(actor, ActivityEventType.ACCOUNT_CREATED,
                "Created account for " + saved.getUsername() + " with role " + saved.getRole());
        return saved;
    }

    /**
     * Mở khóa tài khoản bị LOCKED.
     * Reset failed_login_count về 0 và xóa locked_at.
     */
    @Transactional
    public void unlockUser(Integer userId, User actor) {
        User user = requireUser(userId);
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new IllegalStateException("Only locked accounts can be unlocked");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount((short) 0);
        user.setLockedAt(null);
        // updatedAt sẽ được set bởi @PreUpdate trong User entity
        userRepository.save(user);
        logActivity(actor, ActivityEventType.ACCOUNT_UNLOCKED, "Unlocked account " + user.getUsername());
    }

    /**
     * Deactivate tài khoản (ACTIVE/LOCKED → INACTIVE).
     * Không được deactivate admin cuối cùng còn hoạt động.
     */
    @Transactional
    public void deactivateUser(Integer userId, User actor) {
        User user = requireUser(userId);
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.LOCKED) {
            throw new IllegalStateException("Only active or locked accounts can be deactivated");
        }
        if (user.getRole() == UserRole.ADMIN && isLastActiveAdmin(user)) {
            throw new IllegalStateException("Cannot deactivate the last active admin account");
        }
        user.setStatus(UserStatus.INACTIVE);
        // updatedAt sẽ được set bởi @PreUpdate
        userRepository.save(user);
        logActivity(actor, ActivityEventType.ACCOUNT_DEACTIVATED, "Deactivated account " + user.getUsername());
    }

    /** Kiểm tra xem có thể deactivate user này không — dùng để render action button trên UI */
    @Transactional(readOnly = true)
    public boolean canDeactivate(User user) {
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.LOCKED) {
            return false;
        }
        if (user.getRole() == UserRole.ADMIN && isLastActiveAdmin(user)) {
            return false;
        }
        return true;
    }

    /** Kiểm tra xem có thể unlock user này không — dùng để render action button trên UI */
    @Transactional(readOnly = true)
    public boolean canUnlock(User user) {
        return user.getStatus() == UserStatus.LOCKED;
    }

    /**
     * Admin cuối cùng: không có admin nào đang ACTIVE hoặc LOCKED khác.
     * Nghĩa là nếu chỉ còn 1 admin (ACTIVE hoặc LOCKED), không được deactivate.
     */
    private boolean isLastActiveAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return false;
        }
        long activeAdmins = userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
        long lockedAdmins = userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.LOCKED);
        return (activeAdmins + lockedAdmins) <= 1;
    }

    private User requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Ghi log hoạt động vào bảng activity_log.
     * ipAddress không set ở đây (cần HttpServletRequest nếu muốn, để null là an toàn).
     */
    private void logActivity(User actor, ActivityEventType eventType, String description) {
        ActivityLog log = new ActivityLog();
        log.setActor(actor);
        log.setActorUsername(actor.getUsername());
        log.setEventType(eventType);
        log.setDescription(description);
        // createdAt sẽ được set bởi @PrePersist trong ActivityLog entity
        activityLogRepository.save(log);
    }
}
