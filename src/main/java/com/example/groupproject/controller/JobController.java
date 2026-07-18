package com.example.groupproject.controller;

import com.example.groupproject.dto.JobFormDTO;
import com.example.groupproject.dto.JobListRow;
<<<<<<< HEAD
import com.example.groupproject.dto.ApplicationForm;
=======
import com.example.groupproject.entity.JobPosting;
>>>>>>> cuongnphe194338
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.JobManagementService;
import com.example.groupproject.service.JobService;
import com.example.groupproject.service.ApplicationService;
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

    @Autowired
    private JobManagementService jobService;
    @Autowired
    private AuthService authService;
    @Autowired
    private JobService publicJobService;
    @Autowired
    private ApplicationService applicationService;

    @GetMapping({"", "/"})
    public String listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String location,
            HttpSession session, Model model) {
            
        // 1. Lấy thông tin user đăng nhập hiện tại từ session
        User currentUser = authService.getCurrentUser(session);
        
        // 2. Nếu chưa đăng nhập hoặc là CANDIDATE, hiển thị public job list (SCR-13)
        if (currentUser == null || currentUser.getRole() == UserRole.CANDIDATE) {
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

<<<<<<< HEAD
    @GetMapping({"/create", "/new"})
    public String createJobForm(Model model, HttpSession session) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);
        
        JobFormDTO job = new JobFormDTO();
        model.addAttribute("job", job);
        model.addAttribute("mode", "create");
        return "jobs/form";
    }

    @GetMapping("/edit/{id}")
    public String editJobForm(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);
        
        try {
            com.example.groupproject.entity.JobPosting jobPosting = jobService.getJobById(id, currentUser);
            JobFormDTO dto = new JobFormDTO();
            dto.setId(jobPosting.getId().longValue());
            dto.setTitle(jobPosting.getTitle());
            dto.setDepartment(jobPosting.getDepartment());
            dto.setLocation(jobPosting.getLocation());
            dto.setDescription(jobPosting.getDescription());
            dto.setRequirements(jobPosting.getRequirements());
            dto.setSalaryRange(jobPosting.getSalaryRange());
            dto.setDeadline(jobPosting.getApplicationDeadline());
            dto.setStatus(jobPosting.getStatus().name());
            
            model.addAttribute("job", dto);
            model.addAttribute("mode", "edit");
            return "jobs/form";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/jobs";
        }
    }

    @PostMapping("/save")
    public String saveJob(@Valid @ModelAttribute("job") JobFormDTO jobForm,
                          BindingResult bindingResult,
                          @RequestParam(required = false) String action,
                          HttpSession session,
                          Model model, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        authService.requireAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", jobForm.getId() == null ? "create" : "edit");
            return "jobs/form";
        }

        try {
            com.example.groupproject.entity.JobPosting savedJob = jobService.saveJob(jobForm, currentUser);
            
            if ("publish".equals(action)) {
                jobService.publishJob(savedJob.getId(), currentUser);
                ra.addFlashAttribute("successMessage", "Job posting published successfully!");
            } else {
                ra.addFlashAttribute("successMessage", "Job posting saved successfully.");
            }
            return "redirect:/jobs/" + savedJob.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("mode", jobForm.getId() == null ? "create" : "edit");
            return "jobs/form";
        }
    }

=======
<<<<<<< HEAD
>>>>>>> main
    @GetMapping("/{id}")
    public String publicJobDetail(@PathVariable Integer id, HttpSession session, Model model) {
        User currentUser = authService.getCurrentUser(session);
        boolean isStaff = (currentUser != null && (currentUser.getRole() == UserRole.HR_MANAGER || currentUser.getRole() == UserRole.ADMIN));

        if (isStaff) {
            try {
                com.example.groupproject.entity.JobPosting authJob = jobService.getJobById(id, currentUser);
                model.addAttribute("job", authJob);
                model.addAttribute("applications", applicationService.getApplicationsForJob(id, "ALL", currentUser));
                return "jobs/detail";
            } catch (IllegalStateException e) {
                // HR Manager view another's job, fallback to public view below
            }
        }

        com.example.groupproject.entity.JobPosting job = publicJobService.getJobById(id);
        if (job == null) {
            return "redirect:/jobs";
        }

        boolean isGuest = (currentUser == null);
        boolean isCandidate = (currentUser != null && currentUser.getRole() == UserRole.CANDIDATE);
        boolean isClosed = (job.getStatus() != com.example.groupproject.entity.enums.JobStatus.ACTIVE);

        // Draft postings are only visible to HR Managers/Admins
        if (job.getStatus() == com.example.groupproject.entity.enums.JobStatus.DRAFT) {
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
            redirectAttributes.addFlashAttribute("successMessage", "Your application has been submitted successfully. Track its status in My Applications.");
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
=======
    // Hiển thị chi tiết công việc (Job Detail)
    @GetMapping("/{id}")
    public String showJobDetail(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        try {
            JobPosting job = jobService.getJobById(id, currentUser);
            List<com.example.groupproject.entity.Application> applications = jobService.getApplicationsForJob(id, currentUser);
            model.addAttribute("job", job);
            model.addAttribute("applications", applications);
            return "jobs/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/jobs";
        }
    }

    // 1. Hiển thị form tạo mới
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        JobFormDTO jobForm = new JobFormDTO();
        jobForm.setStatus("DRAFT"); // Mặc định là DRAFT
        model.addAttribute("job", jobForm);
        model.addAttribute("mode", "create");
        return "jobs/form"; // Trỏ tới src/main/resources/templates/jobs/form.html
    }

    // 2. Hiển thị form chỉnh sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        try {
            JobPosting job = jobService.getJobById(id.intValue(), currentUser);
            JobFormDTO jobForm = new JobFormDTO();
            jobForm.setId(job.getId().longValue());
            jobForm.setTitle(job.getTitle());
            jobForm.setDepartment(job.getDepartment());
            jobForm.setLocation(job.getLocation());
            jobForm.setDescription(job.getDescription());
            jobForm.setRequirements(job.getRequirements());
            jobForm.setSalaryRange(job.getSalaryRange());
            jobForm.setDeadline(job.getApplicationDeadline());
            jobForm.setStatus(job.getStatus().name());

            model.addAttribute("job", jobForm);
            model.addAttribute("mode", "edit");
            return "jobs/form";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/jobs";
        }
    }

    // 3. Xử lý submit form
    @PostMapping("/save")
    public String saveJob(
            @Valid @ModelAttribute("job") JobFormDTO jobForm,
            BindingResult bindingResult,
            @RequestParam(value = "action", required = true) String action,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = authService.getCurrentUser(session);

        // Nếu có lỗi nhập liệu, trả lại trang form
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", jobForm.getId() == null ? "create" : "edit");
            return "jobs/form";
        }

        // Kiểm tra logic dựa trên nút người dùng bấm (name="action")
        if ("publish".equals(action)) {
            jobForm.setStatus("ACTIVE");
        } else if ("saveDraft".equals(action)) {
            jobForm.setStatus("DRAFT");
        }
        // saveChanges giữ nguyên trạng thái ACTIVE hiện tại

        try {
            JobPosting savedJob = jobService.saveJob(jobForm, currentUser);
            String message = "saveDraft".equals(action) ? "Job posting saved as Draft." : "Job saved successfully.";
            redirectAttributes.addFlashAttribute("successMessage", message);
            // Since Job Detail page is not yet implemented, redirect to job list
            return "redirect:/jobs"; 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (jobForm.getId() != null) {
                return "redirect:/jobs/edit/" + jobForm.getId();
            } else {
                return "redirect:/jobs/create";
            }
>>>>>>> cuongnphe194338
        }
    }
}
