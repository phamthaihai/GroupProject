package com.example.groupproject.config;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice inject currentUser và role flags vào tất cả Model.
 *
 * @Transactional(readOnly = true) keeps the Hibernate session open for the
 * duration of all @ModelAttribute methods on this advice. Without it, the User
 * entity returned by getCurrentUser() is detached before Thymeleaf renders,
 * causing LazyInitializationException if any lazy-loaded field is accessed.
 */
@ControllerAdvice
@Transactional(readOnly = true)
public class GlobalModelAdvice {

    private final AuthService authService;

    public GlobalModelAdvice(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(HttpSession session) {
        return authService.getCurrentUser(session);
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(HttpSession session) {
        return authService.getCurrentUser(session) != null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpSession session) {
        User user = authService.getCurrentUser(session);
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    @ModelAttribute("canAccessHr")
    public boolean canAccessHr(HttpSession session) {
        User user = authService.getCurrentUser(session);
        return user != null && authService.hasAnyRole(user, UserRole.ADMIN, UserRole.HR_MANAGER);
    }
}
