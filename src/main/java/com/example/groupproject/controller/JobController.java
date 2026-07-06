package com.example.groupproject.controller;

import com.example.groupproject.dto.JobListRow;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.JobManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @Autowired
    private JobManagementService jobService;
    @Autowired
    private AuthService authService;

    @GetMapping({"", "/"})
    public String listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            HttpSession session, Model model) {
            
        // 1. Lấy thông tin user đăng nhập hiện tại từ session
        User currentUser = authService.getCurrentUser(session);
        
        // 2. Gọi Service lấy dữ liệu công việc và danh sách phòng ban làm filter dropdown
        List<JobListRow> jobs = jobService.getJobsForList(currentUser, status, keyword, department);
        List<String> departments = jobService.getDistinctDepartments(currentUser);
        Map<String, Long> counts = jobService.getJobCountsByStatus(currentUser);

        // 3. Đẩy dữ liệu ra Model Thymeleaf
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
}
