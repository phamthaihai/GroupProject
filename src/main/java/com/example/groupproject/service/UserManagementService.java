package com.example.groupproject.service;

import com.example.groupproject.dto.CreateUserForm;
import com.example.groupproject.entity.ActivityLog;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.ActivityEventType;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.ActivityLogRepository;
import com.example.groupproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service quản lý vòng đời tài khoản người dùng (Admin use cases).
 */
@Service
public class UserManagementService {

    private static final Set<UserRole> CREATABLE_ROLES = Set.of(UserRole.HR_MANAGER, UserRole.INTERVIEWER);

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    public UserManagementService(UserRepository userRepository,
                                 ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String search, UserRole role, UserStatus status) {
        List<User> users = userRepository.findByRoleAndStatusFiltered(role, status);
        if (search == null || search.trim().isEmpty()) {
            return users;
        }
        String lower = search.trim().toLowerCase(java.util.Locale.ROOT);
        return users.stream()
                .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase(java.util.Locale.ROOT).contains(lower))
                        || (u.getEmail() != null && u.getEmail().toLowerCase(java.util.Locale.ROOT).contains(lower)))
                .toList();
    }

    @Transactional
    public User createUser(CreateUserForm form, User actor) {
        requireRole(actor, UserRole.ADMIN);

        UserRole role = form.getUserRole();
        if (!CREATABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Only HR Manager or Interviewer accounts can be created");
        }
        if (userRepository.findByUsername(form.getUsername()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        // Store password as plain text (no hashing)
        user.setPasswordHash(form.getPassword());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        logActivity(actor, ActivityEventType.ACCOUNT_CREATED,
                "Created account for " + saved.getUsername() + " with role " + saved.getRole());
        return saved;
    }

    @Transactional
    public void unlockUser(Integer userId, User actor) {
        requireRole(actor, UserRole.ADMIN);

        User user = requireUser(userId);
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new IllegalStateException("Only locked accounts can be unlocked");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount((short) 0);
        user.setLockedAt(null);
        userRepository.save(user);
        logActivity(actor, ActivityEventType.ACCOUNT_UNLOCKED, "Unlocked account " + user.getUsername());
    }

    @Transactional
    public void deactivateUser(Integer userId, User actor) {
        requireRole(actor, UserRole.ADMIN);

        User user = requireUser(userId);
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.LOCKED) {
            throw new IllegalStateException("Only active or locked accounts can be deactivated");
        }
        if (user.getRole() == UserRole.ADMIN && isLastActiveAdmin(user)) {
            throw new IllegalStateException("Cannot deactivate the last active admin account");
        }
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        logActivity(actor, ActivityEventType.ACCOUNT_DEACTIVATED, "Deactivated account " + user.getUsername());
    }

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

    @Transactional(readOnly = true)
    public boolean canUnlock(User user) {
        return user.getStatus() == UserStatus.LOCKED;
    }

    private void requireRole(User actor, UserRole role) {
        if (actor == null || actor.getRole() != role) {
            throw new IllegalStateException("Access denied");
        }
    }

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

    private void logActivity(User actor, ActivityEventType eventType, String description) {
        ActivityLog log = new ActivityLog();
        log.setActor(actor);
        log.setActorUsername(actor.getUsername());
        log.setEventType(eventType);
        log.setDescription(description);
        activityLogRepository.save(log);
    }
}
