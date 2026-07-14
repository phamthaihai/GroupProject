package com.example.groupproject.controller;

import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.service.ApplicationService;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.DashboardService;
import com.example.groupproject.service.JobService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller cho HR Dashboard.
 */
@Controller
@RequestMapping("/hr")
public class HrDashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final ApplicationRepository applicationRepository;

    public HrDashboardController(DashboardService dashboardService,
                                 AuthService authService,
                                 ApplicationService applicationService,
                                 JobService jobService,
                                 ApplicationRepository applicationRepository) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.applicationService = applicationService;
        this.jobService = jobService;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);
        model.addAttribute("summary", dashboardService.getRecruitmentSummary(currentUser));
        model.addAttribute("activeJobs", dashboardService.getActiveJobRows(currentUser));
        return "hr/dashboard";
    }

    @GetMapping("/jobs/{jobId}/applications")
    public String getApplications(@PathVariable Integer jobId,
                                  @RequestParam(required = false, defaultValue = "ALL") String status,
                                  @RequestHeader(value = "HX-Request", required = false) boolean hxRequest,
                                  HttpSession session, Model model) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);

        JobPosting job = jobService.getJobById(jobId);
        if (job == null) {
            return "redirect:/hr/dashboard";
        }

        List<Application> apps = applicationService.getApplicationsForJob(jobId, status, currentUser);
        Map<String, Long> counts = applicationService.getApplicationCountsByStage(jobId);

        model.addAttribute("job", job);
        model.addAttribute("applications", apps);
        model.addAttribute("counts", counts);
        model.addAttribute("currentStatus", status.toUpperCase());

        if (hxRequest) {
            return "hr/applications :: applicantList";
        }
        return "hr/applications";
    }

    @GetMapping("/report/{jobId}")
    public String showPipelineReport(@PathVariable Integer jobId, Model model) {
        List<Object[]> statusCounts = applicationRepository.countApplicationsByStatusAndJobId(jobId);
        List<Application> applications = applicationRepository.findByJobId(jobId);

        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("applications", applications);

        return "hr/report";
    }
}