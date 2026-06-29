package com.example.groupproject.controller;

import com.example.groupproject.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho Admin Dashboard.
 * Chỉ ADMIN được truy cập (/admin/** được bảo vệ bởi SecurityConfig).
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /** GET /admin/dashboard — hiển thị admin dashboard với stats và recent activity */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.getAdminDashboardData());
        model.addAttribute("recentActivity", dashboardService.getRecentActivityEvents());
        return "admin/dashboard";
    }

    /** GET /admin/activity-log — hiển thị trang activity log */
    @GetMapping("/activity-log")
    public String activityLog() {
        return "admin/activity-log";
    }
}
