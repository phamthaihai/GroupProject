package com.example.groupproject.service;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session-based authentication and manual authorization checks.
 */
@Service
public class AuthService {

    public static final String SESSION_USER_ID = "loggedInUserId";

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof Integer userId)) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public boolean login(String username, String password, HttpSession session) {
        if (username == null || password == null || session == null) {
            return false;
        }

        User user = userRepository.findByUsername(username.trim());
        if (user == null) {
            return false;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        // Direct plain text password comparison
        if (!password.equals(user.getPassword())) {
            return false;
        }

        session.setAttribute(SESSION_USER_ID, user.getId());
        return true;
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public boolean isAuthenticated(HttpSession session) {
        return getCurrentUser(session) != null;
    }

    public boolean hasRole(User user, UserRole role) {
        return user != null && user.getRole() == role;
    }

    public boolean hasAnyRole(User user, UserRole... roles) {
        if (user == null || roles == null) {
            return false;
        }
        for (UserRole role : roles) {
            if (user.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    public void requireAuthenticated(User user) {
        if (user == null) {
            throw new IllegalStateException("Authentication required");
        }
    }

    public void requireRole(User user, UserRole role) {
        requireAuthenticated(user);
        if (user.getRole() != role) {
            throw new IllegalStateException("Access denied");
        }
    }

    public void requireAnyRole(User user, UserRole... roles) {
        requireAuthenticated(user);
        if (!hasAnyRole(user, roles)) {
            throw new IllegalStateException("Access denied");
        }
    }
}
