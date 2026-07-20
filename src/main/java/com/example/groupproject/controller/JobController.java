package com.example.groupproject.controller;

import com.example.groupproject.dto.JobFormDTO;
import com.example.groupproject.dto.JobListRow;
import com.example.groupproject.dto.ApplicationForm;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.JobManagementService;
import com.example.groupproject.service.JobService;
import com.example.groupproject.service.ApplicationService;
import com.example.groupproject.entity.Application;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller cho Job Posting management.
 * Yêu cầu role ADMIN hoặc HR_MANAGER (kiểm tra bởi AuthInterceptor: /jobs/**).
 */
@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobManagementService jobService;
    private final AuthService authService;

   // @Autowired
    //private JobManagementService jobService;
   // @Autowired
    //private AuthService authService;
    @Autowired
    private JobService publicJobService;
    @Autowired
    private ApplicationService applicationService;
    public JobController(JobManagementService jobService, AuthService authService) {
        this.jobService = jobService;
        this.authService = authService;
    }

    @GetMapping({"", "/"})
    public String listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String location,
            HttpSession session, Model model) {
            
        // 1. Lấy thông tin user đăng nhập hiện tại từ session
        User currentUser = authService.getCurrentUser(session);
        
        // 2. Nếu chưa đăng nhập hoặc là CANDIDATE/INTERVIEWER, hiển thị public job list (SCR-13)
        if (currentUser == null || currentUser.getRole() == UserRole.CANDIDATE || currentUser.getRole() == UserRole.INTERVIEWER) {
            JobService.PublicJobListData data = publicJobService.getPublicJobList(department, location);
            model.addAttribute("jobs", data.jobs());
            model.addAttribute("departments", data.departments());
            model.addAttribute("locations", data.locations());
            model.addAttribute("selectedDepartment", data.selectedDepartment());
            model.addAttribute("selectedLocation", data.selectedLocation());
            model.addAttribute("hasActivePostings", data.hasActivePostings());
            model.addAttribute("hasSelectedFilter", data.hasSelectedFilter());
            return "jobs/public-list";
        }
        
        // 3. Gọi Service lấy dữ liệu công việc và danh sách phòng ban làm filter dropdown cho ADMIN/HR
        List<JobListRow> jobs = jobService.getJobsForList(currentUser, status, keyword, department);
        List<String> departments = jobService.getDistinctDepartments(currentUser);
        Map<String, Long> counts = jobService.getJobCountsByStatus(currentUser);

        // 4. Đẩy dữ liệu ra Model Thymeleaf
        model.addAttribute("jobs", jobs);
        model.addAttribute("departments", departments);
        model.addAttribute("counts", counts);
        model.addAttribute("currentStatus", status != null ? status.name() : "ALL");
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentDepartment", department);

        return "jobs/list";
    }

    @PostMapping("/{id}/publish")
    public String publishJob(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        try {
            jobService.publishJob(id, currentUser);
            ra.addFlashAttribute("successMessage", "Tin tuyển dụng đã được đăng hoạt động!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/jobs";
    }

    @PostMapping("/{id}/close")
    public String closeJob(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        try {
            jobService.closeJob(id, currentUser);
            ra.addFlashAttribute("successMessage", "Tin tuyển dụng đã được đóng thành công!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/jobs";
    }

    @PostMapping("/{id}/delete")
    public String deleteJob(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        try {
            jobService.deleteJob(id, currentUser);
            ra.addFlashAttribute("successMessage", "Tin tuyển dụng đã được xóa thành công!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/jobs";
    }

    @GetMapping("/{id}")
    public String publicJobDetail(@PathVariable Integer id, HttpSession session, Model model) {
        com.example.groupproject.entity.JobPosting job = publicJobService.getJobById(id);
        if (job == null) {
            return "redirect:/jobs";
        }

        User currentUser = authService.getCurrentUser(session);
        boolean isGuest = (currentUser == null);
        boolean isCandidate = (currentUser != null && currentUser.getRole() == UserRole.CANDIDATE);
        boolean isClosed = (job.getStatus() != com.example.groupproject.entity.enums.JobStatus.ACTIVE);

        // Draft postings are only visible to HR Managers/Admins
        if (job.getStatus() == com.example.groupproject.entity.enums.JobStatus.DRAFT) {
            boolean isStaff = (currentUser != null && (currentUser.getRole() == UserRole.HR_MANAGER || currentUser.getRole() == UserRole.ADMIN));
            if (!isStaff) {
                return "redirect:/jobs";
            }
        }

        boolean hasApplied = false;
        if (isCandidate) {
            hasApplied = applicationService.hasApplied(id, currentUser.getId());
        }

        model.addAttribute("job", job);
        model.addAttribute("isGuest", isGuest);
        model.addAttribute("isCandidate", isCandidate);
        model.addAttribute("isClosed", isClosed);
        model.addAttribute("hasApplied", hasApplied);
        model.addAttribute("applicationForm", new ApplicationForm());

        return "jobs/public-detail";
    }

    @PostMapping("/{id}/apply")
    public String applyJob(@PathVariable Integer id,
                           @ModelAttribute("applicationForm") ApplicationForm form,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null || currentUser.getRole() != UserRole.CANDIDATE) {
            return "redirect:/login";
        }

        try {
            applicationService.applyToJob(id, currentUser, form);
            redirectAttributes.addFlashAttribute("successMessage", "Your application has been submitted successfully. ");
            return "redirect:/jobs/" + id;
        } catch (Exception ex) {
            com.example.groupproject.entity.JobPosting job = publicJobService.getJobById(id);
            model.addAttribute("job", job);
            model.addAttribute("isGuest", false);
            model.addAttribute("isCandidate", true);
            model.addAttribute("isClosed", job.getStatus() != com.example.groupproject.entity.enums.JobStatus.ACTIVE);
            model.addAttribute("hasApplied", false);
            model.addAttribute("errorMessage", ex.getMessage());
            return "jobs/public-detail";
        }
    }

    @GetMapping("/detail/{id}")
    public String viewJobDetailInternal(@PathVariable Integer id, HttpSession session, Model model) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.HR_MANAGER) {
            return "redirect:/jobs";
        }
        try {
            JobPosting job = jobService.getJobById(id, currentUser);
            List<Application> apps = jobService.getApplicationsForJob(id, currentUser);
            model.addAttribute("job", job);
            model.addAttribute("applications", apps);
            return "jobs/detail";
        } catch (Exception e) {
            return "redirect:/jobs";
        }
    }
}
