package com.example.groupproject.security;

import com.example.groupproject.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class để lấy thông tin user đang đăng nhập từ SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // utility class, no instantiation
    }

    /**
     * Lấy User entity của người dùng đang đăng nhập.
     *
     * @return User entity, hoặc null nếu chưa đăng nhập hoặc principal không phải TalentHubUserDetails
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TalentHubUserDetails details) {
            return details.getUser();
        }
        return null;
    }
}
