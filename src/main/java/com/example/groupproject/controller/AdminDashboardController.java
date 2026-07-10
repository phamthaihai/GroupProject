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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import com.example.groupproject.entity.enums.ActivityEventType;
import com.example.groupproject.entity.view.ActivityLogDisplayView;
import org.springframework.data.domain.Page;
import java.time.LocalDate;

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

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
        model.addAttribute("dashboard", dashboardService.getAdminDashboardData());
        model.addAttribute("recentActivity", dashboardService.getRecentActivityEvents());
        return "admin/dashboard";
    }

    @GetMapping("/activity-log")
    public String activityLog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ActivityEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {
        
        // Kiểm tra quyền ADMIN
        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
        
        // Gọi Service lấy dữ liệu phân trang
        Page<ActivityLogDisplayView> logsPage = dashboardService.searchActivityLogs(search, eventType, dateFrom, dateTo, page);
        
        // Đưa các thuộc tính vào Model để hiển thị lên Thymeleaf
        model.addAttribute("logsPage", logsPage);
        model.addAttribute("search", search);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("currentPage", page);
        model.addAttribute("eventTypes", ActivityEventType.values());
        
        return "admin/activity-log";
    }
}
