package com.example.groupproject.config;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Replaces Spring Security URL authorization with manual session checks.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            return true;
        }
        User user = authService.getCurrentUser(request.getSession(false));
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }

        if (path.startsWith("/admin/") && user.getRole() != UserRole.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        if ((path.startsWith("/hr/") || path.startsWith("/jobs/"))
                && user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.HR_MANAGER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/login")
                || path.equals("/register")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.equals("/error");
    }
}
