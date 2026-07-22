package com.example.groupproject.controller;

import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.Interview;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.InterviewRepository;
import com.example.groupproject.repository.JobPostingRepository;
import com.example.groupproject.service.ApplicationService;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.DashboardService;
import com.example.groupproject.service.JobService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller cho HR Dashboard & Pipeline Report (SCR-20).
 */
@Controller
@RequestMapping("/hr")
@Transactional(readOnly = true)
public class HrDashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final InterviewRepository interviewRepository;

    public HrDashboardController(DashboardService dashboardService,
                                 AuthService authService,
                                 ApplicationService applicationService,
                                 JobService jobService,
                                 ApplicationRepository applicationRepository,
                                 JobPostingRepository jobPostingRepository,
                                 InterviewRepository interviewRepository) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.applicationService = applicationService;
        this.jobService = jobService;
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.interviewRepository = interviewRepository;
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

    // === 1. PIPELINE REPORT TỪ DROPDOWN HOẶC ĐIỀU HƯỚNG CHUNG (SCR-20) ===
    @GetMapping("/report")
    public String showPipelineReportDefault(@RequestParam(required = false) Integer jobId, HttpSession session, Model model) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);

        List<JobPosting> jobs = (currentUser.getRole() == UserRole.ADMIN)
                ? jobPostingRepository.findAll()
                : jobPostingRepository.findByCreatedById(currentUser.getId());

        if (jobs.isEmpty()) {
            model.addAttribute("applications", Collections.emptyList());
            return "hr/report";
        }

        Integer targetJobId = (jobId != null) ? jobId : jobs.get(0).getId();
        return populateReportModel(targetJobId, jobs, model);
    }

    // === 2. PIPELINE REPORT THEO JOB ID CỤ THỂ (SCR-20) ===
    @GetMapping("/report/{jobId}")
    public String showPipelineReport(@PathVariable Integer jobId, HttpSession session, Model model) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);

        List<JobPosting> jobs = (currentUser.getRole() == UserRole.ADMIN)
                ? jobPostingRepository.findAll()
                : jobPostingRepository.findByCreatedById(currentUser.getId());

        return populateReportModel(jobId, jobs, model);
    }

    // HÀM Bổ SUNG NẠP ĐẦY ĐỦ DỮ LIỆU BÁO CÁO CHO MODEL
    private String populateReportModel(Integer jobId, List<JobPosting> jobs, Model model) {
        List<Application> applications = applicationRepository.findByJobId(jobId);

        // Map đếm số lượng cho 7 stage để vẽ Horizontal Bar Chart theo Spec
        Map<String, Long> stageCountsMap = new HashMap<>();
        stageCountsMap.put("APPLIED", 0L);
        stageCountsMap.put("SCREENING", 0L);
        stageCountsMap.put("INTERVIEW", 0L);
        stageCountsMap.put("OFFER", 0L);
        stageCountsMap.put("HIRED", 0L);
        stageCountsMap.put("REJECTED", 0L);
        stageCountsMap.put("WITHDRAWN", 0L);

        // Map dữ liệu tính toán thêm cho từng Candidate (Days in stage, Assigned Interviewer)
        List<Map<String, Object>> candidateDataList = new ArrayList<>();

        for (Application app : applications) {
            String status = app.getStatus() != null ? app.getStatus().name() : "APPLIED";
            stageCountsMap.put(status, stageCountsMap.getOrDefault(status, 0L) + 1);

            Map<String, Object> data = new HashMap<>();
            data.put("id", app.getId());
            data.put("candidateName", app.getCandidate() != null ? app.getCandidate().getFullName() : "N/A");
            data.put("status", status);

            // Tính số ngày ở trạng thái hiện tại (Days in stage)
            Instant changedAt = app.getStatusChangedAt() != null ? app.getStatusChangedAt() : app.getSubmittedAt();
            long daysInStage = (changedAt != null) ? ChronoUnit.DAYS.between(changedAt, Instant.now()) : 0;
            data.put("daysInStage", Math.max(0, daysInStage));

            // Lấy Tên Interviewer được gán (Assigned Interviewer)
            List<Interview> interviews = interviewRepository.findByApplicationId(app.getId());
            String interviewerName = "None";
            if (!interviews.isEmpty() && interviews.get(0).getInterviewer() != null) {
                interviewerName = interviews.get(0).getInterviewer().getFullName();
            }
            data.put("assignedInterviewerName", interviewerName);

            candidateDataList.add(data);
        }

        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("jobs", jobs);
        model.addAttribute("applications", candidateDataList);
        model.addAttribute("stageCountsMap", stageCountsMap);

        return "hr/report";
    }
}