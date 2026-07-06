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
 * Controller cho HR Dashboard.
 */
@Controller
@RequestMapping("/hr")
public class HrDashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    public HrDashboardController(DashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);
        model.addAttribute("summary", dashboardService.getRecruitmentSummary(currentUser));
        model.addAttribute("activeJobs", dashboardService.getActiveJobRows(currentUser));
        return "hr/dashboard";
    }
}
