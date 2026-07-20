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
        //kiểm tra đường dẫn có phải là đường không thầm quyền
        System.out.println("LOG: Đang truy cập path: " + path);
        if (isPublicPath(path)) {
            return true;
        }
        //Kiểm tra path bằng cách in ra
        System.out.println("URI = " + request.getRequestURI());
        System.out.println("Context = " + request.getContextPath());

        User user = authService.getCurrentUser(request.getSession(false));
        //Kiểm tra có người dùng đăng nhập
        if (user == null) {
            System.out.println("LOG: User null -> Redirect về /login");
            response.sendRedirect("/login");
            return false;
        }
        //kiểm tra role của user đối với path
        //FOR ADMIN ONLY PATH
        if (path.startsWith("/admin/") && user.getRole() != UserRole.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        // FOR CANDIDATE ONLY PATH
        if (path.startsWith("/candidate/") && user.getRole() != UserRole.CANDIDATE) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        // FOR CANDIDATE APPLY PATH
        if (path.matches("/jobs/\\d+/apply") && user.getRole() == UserRole.CANDIDATE) {
            return true;
        }

        //FOR ADMIN & HR_MANAGER PATH
        if ((path.startsWith("/hr/") || path.startsWith("/jobs/"))
                && user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.HR_MANAGER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        // --- CHÈN ĐOẠN NÀY VÀO TRƯỚC LỆNH "return true;" CUỐI CÙNG ---

        // Cho phép truy cập vào các đường dẫn ứng viên (SCR-17)
        if (path.startsWith("/applications/")) {
            // Chỉ ADMIN, HR_MANAGER, INTERVIEWER mới được xem
            if (user.getRole() == UserRole.ADMIN ||
                    user.getRole() == UserRole.HR_MANAGER ||
                    user.getRole() == UserRole.INTERVIEWER) {
                return true;
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN); // Báo lỗi quyền truy cập
                return false;
            }
        }
        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/login")
                || path.startsWith("/register")
                || path.equals("/verify")              // <--- THÊM DÒNG NÀY VÀO ĐÂY!
                || path.equals("/")
                || path.equals("/jobs")
                || path.equals("/jobs/")
                || path.matches("/jobs/\\d+")
                || path.equals("/forgot-password")
                || path.equals("/reset-password")
                || path.startsWith("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.equals("/error");
    }
}
