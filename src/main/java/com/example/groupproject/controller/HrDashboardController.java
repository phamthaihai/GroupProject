package com.example.groupproject.controller;

import com.example.groupproject.entity.User;
import com.example.groupproject.security.SecurityUtils;
import com.example.groupproject.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho HR Dashboard.
 * HR_MANAGER chỉ xem data của job do mình tạo (scoped view).
 * ADMIN cũng có thể truy cập /hr/** theo SecurityConfig.
 */
@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrDashboardController {

    private final DashboardService dashboardService;

    /** GET /hr/dashboard — hiển thị HR dashboard với summary và active jobs */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = SecurityUtils.getCurrentUser();
        model.addAttribute("summary", dashboardService.getRecruitmentSummary(currentUser));
        model.addAttribute("activeJobs", dashboardService.getActiveJobRows(currentUser));
        return "hr/dashboard";
    }
}
