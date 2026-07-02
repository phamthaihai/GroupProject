package com.example.groupproject.controller;

import com.example.groupproject.dto.JobFormDto;
import com.example.groupproject.dto.JobListRow;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.JobManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final JobManagementService jobManagementService;
    private final AuthService authService;

    public JobController(JobManagementService jobManagementService, AuthService authService) {
        this.jobManagementService = jobManagementService;
        this.authService = authService;
    }

    /** GET /jobs — danh sách job (SCR-10) */
//    @GetMapping({"", "/"})
//    public String listJobs(@RequestParam(required = false) JobStatus status,
//                           @RequestParam(required = false) String keyword,
//                           @RequestParam(required = false) String department,
//                           jakarta.servlet.http.HttpSession session,
//                           Model model) {
//        User currentUser = authService.getCurrentUser(session);
//
//        List<JobListRow> jobs = jobManagementService.getJobsForList(currentUser, status, keyword, department);
//        List<String> departments = jobManagementService.getDistinctDepartments(currentUser);
//        Map<String, Long> counts = jobManagementService.getJobCountsByStatus(currentUser);
//
//        model.addAttribute("jobs", jobs);
//        model.addAttribute("departments", departments);
//        model.addAttribute("counts", counts);
//        model.addAttribute("currentStatus", status != null ? status.name() : "ALL");
//        model.addAttribute("currentKeyword", keyword);
//        model.addAttribute("currentDepartment", department);
//
//        return "jobs/list";
//    }
//
//    /** GET /jobs/new — hiển thị form tạo job mới */
//    @GetMapping("/new")
//    public String createJobForm(Model model) {
//        model.addAttribute("jobDto", new JobFormDto());
//        model.addAttribute("mode", "create");
//        return "jobs/form";
//    }
//
//    @PostMapping("/new")
//    public String createJob(@Valid @ModelAttribute("jobDto") JobFormDto jobDto,
//                            BindingResult bindingResult,
//                            jakarta.servlet.http.HttpSession session,
//                            Model model,
//                            RedirectAttributes redirectAttributes) {
//        if (bindingResult.hasErrors()) {
//            model.addAttribute("mode", "create");
//            return "jobs/form";
//        }
//        User currentUser = authService.getCurrentUser(session);
//        Integer newJobId = jobManagementService.createJobFromDto(jobDto, currentUser);
//        redirectAttributes.addFlashAttribute("successMessage", "Job posting saved as Draft.");
//        return "redirect:/jobs/" + newJobId;
//    }
//
//    @GetMapping("/{id}/edit")
//    public String editJobForm(@PathVariable Integer id, jakarta.servlet.http.HttpSession session, Model model, RedirectAttributes redirectAttributes) {
//        User currentUser = authService.getCurrentUser(session);
//        JobPosting job = jobManagementService.getJobById(id, currentUser);
//        if (job == null) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Job not found or access denied.");
//            return "redirect:/jobs";
//        }
//
//        JobFormDto dto = new JobFormDto();
//        dto.setId(job.getId());
//        dto.setTitle(job.getTitle());
//        dto.setDepartment(job.getDepartment());
//        dto.setLocation(job.getLocation());
//        dto.setDescription(job.getDescription());
//        dto.setRequirements(job.getRequirements());
//        dto.setSalaryRange(job.getSalaryRange());
//        dto.setApplicationDeadline(job.getApplicationDeadline());
//
//        model.addAttribute("jobDto", dto);
//        model.addAttribute("mode", "edit");
//        model.addAttribute("jobStatus", job.getStatus().name());
//        return "jobs/form";
//    }
//
//    @PostMapping("/{id}/edit")
//    public String editJob(@PathVariable Integer id,
//                          @Valid @ModelAttribute("jobDto") JobFormDto jobDto,
//                          BindingResult bindingResult,
//                          jakarta.servlet.http.HttpSession session,
//                          Model model,
//                          RedirectAttributes redirectAttributes) {
//        User currentUser = authService.getCurrentUser(session);
//        JobPosting job = jobManagementService.getJobById(id, currentUser);
//
//        if (job == null) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Job not found or access denied.");
//            return "redirect:/jobs";
//        }
//
//        if (bindingResult.hasErrors()) {
//            model.addAttribute("mode", "edit");
//            model.addAttribute("jobStatus", job.getStatus().name());
//            return "jobs/form";
//        }
//
//        try {
//            jobManagementService.updateJobFromDto(id, jobDto, currentUser);
//            redirectAttributes.addFlashAttribute("successMessage", "Job posting updated.");
//        } catch (IllegalStateException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//        }
//
//        return "redirect:/jobs/" + id;
//    }
//
//    @PostMapping("/{id}/publish")
//    public String publishJob(@PathVariable Integer id, jakarta.servlet.http.HttpSession session, RedirectAttributes redirectAttributes) {
//        User currentUser = authService.getCurrentUser(session);
//        jobManagementService.publishJob(id, currentUser);
//        redirectAttributes.addFlashAttribute("successMessage", "Job posting published successfully.");
//        return "redirect:/jobs";
//    }
//
//    @PostMapping("/{id}/delete")
//    public String deleteJob(@PathVariable Integer id, jakarta.servlet.http.HttpSession session, RedirectAttributes redirectAttributes) {
//        User currentUser = authService.getCurrentUser(session);
//        try {
//            jobManagementService.deleteJob(id, currentUser);
//            redirectAttributes.addFlashAttribute("successMessage", "Job posting deleted successfully.");
//        } catch (IllegalStateException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//        }
//        return "redirect:/jobs";
//    }
//
//    @PostMapping("/{id}/close")
//    public String closeJob(@PathVariable Integer id, jakarta.servlet.http.HttpSession session, RedirectAttributes redirectAttributes) {
//        User currentUser = authService.getCurrentUser(session);
//        jobManagementService.closeJob(id, currentUser);
//        redirectAttributes.addFlashAttribute("successMessage", "Job posting closed successfully.");
//        return "redirect:/jobs";
//    }
}
