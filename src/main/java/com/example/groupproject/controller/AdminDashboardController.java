package com.example.groupproject.controller;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho Admin Dashboard.
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    public AdminDashboardController(DashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

//    @GetMapping("/dashboard")
//    public String dashboard(Model model, HttpSession session) {
//        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
//        model.addAttribute("dashboard", dashboardService.getAdminDashboardData());
//        model.addAttribute("recentActivity", dashboardService.getRecentActivityEvents());
//        return "admin/dashboard";
//    }
//
//    @GetMapping("/activity-log")
//    public String activityLog(HttpSession session) {
//        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
//        return "admin/activity-log";
//    }
}
