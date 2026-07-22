package com.example.groupproject.controller;

import com.example.groupproject.dto.InterviewAssignmentDTO;
import com.example.groupproject.entity.ActivityLog;
import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.Interview;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.ActivityEventType;
import com.example.groupproject.entity.enums.InterviewStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.ActivityLogRepository;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.InterviewRepository;
import com.example.groupproject.repository.UserRepository;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@Transactional
public class InterviewController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final AuthService authService;
    private final ActivityLogRepository activityLogRepository;

    public InterviewController(UserRepository userRepository, ApplicationRepository applicationRepository,
                               InterviewRepository interviewRepository, AuthService authService,
                               ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.authService = authService;
        this.activityLogRepository = activityLogRepository;
    }

    // 1. HIỆN FORM ASSIGN (SCR-18)
    @GetMapping({"/hr/applications/{applicationId}/assign", "/interview/assign/{applicationId}"})
    public String showAssignForm(@PathVariable Integer applicationId, HttpSession session, Model model, RedirectAttributes redirect) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) return "redirect:/login";

        boolean hasPermission = authService.hasAnyRole(currentUser, UserRole.ADMIN, UserRole.HR_MANAGER);
        if (!hasPermission) {
            redirect.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này.");
            return "redirect:/applications/" + applicationId;
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ID"));

        List<User> interviewers = userRepository.findByRoleAndStatusOrderByFullNameAsc(UserRole.INTERVIEWER, UserStatus.ACTIVE);

        model.addAttribute("app", app);
        model.addAttribute("interviewers", interviewers);

        InterviewAssignmentDTO dto = new InterviewAssignmentDTO();
        dto.setApplicationId(applicationId);
        if (app.getCandidate() != null) dto.setCandidateName(app.getCandidate().getFullName());
        if (app.getJob() != null) dto.setJobTitle(app.getJob().getTitle());

        model.addAttribute("assignment", dto);

        return "hr/assign-interview";
    }

    // 2. XỬ LÝ LƯU PHỎNG VẤN (SCR-18)
    @PostMapping("/interview/assign")
    public String processAssignment(@ModelAttribute("assignment") InterviewAssignmentDTO dto,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirect) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) return "redirect:/login";

        LocalDate interviewDate = dto.getInterviewDate();

        if (interviewDate != null && interviewDate.isBefore(LocalDate.now())) {
            Application app = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid ID"));
            List<User> interviewers = userRepository.findByRoleAndStatusOrderByFullNameAsc(UserRole.INTERVIEWER, UserStatus.ACTIVE);

            model.addAttribute("app", app);
            model.addAttribute("interviewers", interviewers);
            model.addAttribute("dateError", "Interview must be scheduled for a future date and time.");
            return "hr/assign-interview";
        }

        LocalTime interviewTime = LocalTime.parse(dto.getInterviewTime());

        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Application ID"));
        User interviewer = userRepository.findById(dto.getInterviewerId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Interviewer ID"));

        Interview interview = new Interview();
        interview.setApplication(app);
        interview.setInterviewer(interviewer);
        interview.setInterviewDate(interviewDate);
        interview.setInterviewTime(interviewTime);
        interview.setLocationOrLink(dto.getLocationOrLink());
        interview.setAssignedBy(currentUser);

        interviewRepository.save(interview);

        redirect.addFlashAttribute("successMessage",
                "Interview scheduled. " + interviewer.getFullName() + " has been assigned.");

        return "redirect:/applications/" + dto.getApplicationId();
    }

    // 3. HIỆN FORM ĐÁNH GIÁ (SCR-19) - ĐÃ CỦNG CỐ CHECK QUYỀN VÀ SPEC
    @GetMapping("/interview/evaluate/{interviewId}")
    public String showEvaluateForm(@PathVariable Integer interviewId, HttpSession session, Model model, RedirectAttributes redirect) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) return "redirect:/login";

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Interview ID"));

        // SPEC CHECK 1: Interviewer not assigned to this application -> "Access denied"
        boolean isAssigned = (interview.getInterviewer() != null && interview.getInterviewer().getId().equals(currentUser.getId()))
                || currentUser.getRole() == UserRole.ADMIN;

        if (!isAssigned) {
            redirect.addFlashAttribute("errorMessage", "Access denied");
            return "redirect:/applications/" + (interview.getApplication() != null ? interview.getApplication().getId() : "");
        }

        boolean isEvaluated = (interview.getStatus() == InterviewStatus.EVALUATED);

        // Format ngày gửi đánh giá hiển thị ở dạng Read-only nếu đã submit
        String formattedEvaluatedDate = "";
        if (isEvaluated && interview.getEvaluatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());
            formattedEvaluatedDate = formatter.format(interview.getEvaluatedAt());
        }

        model.addAttribute("interview", interview);
        model.addAttribute("isEvaluated", isEvaluated);
        model.addAttribute("evaluatedDate", formattedEvaluatedDate);

        return "hr/evaluate-interview";
    }

    // 4. XỬ LÝ LƯU ĐÁNH GIÁ (SCR-19) - ĐÃ CỦNG CỐ CẢNH BÁO VÀ IMMUTABLE
    @PostMapping("/interview/evaluate")
    public String processEvaluation(@RequestParam Integer interviewId,
                                    @RequestParam Short rating,
                                    @RequestParam String feedback,
                                    HttpSession session,
                                    RedirectAttributes redirect) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) return "redirect:/login";

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ID"));

        // SPEC CHECK 1: Security check
        boolean isAssigned = (interview.getInterviewer() != null && interview.getInterviewer().getId().equals(currentUser.getId()))
                || currentUser.getRole() == UserRole.ADMIN;
        if (!isAssigned) {
            redirect.addFlashAttribute("errorMessage", "Access denied");
            return "redirect:/applications/" + interview.getApplication().getId();
        }

        // SPEC CHECK 2: Immutable check (Nếu đã EVALUATED thì không cho sửa nữa)
        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            redirect.addFlashAttribute("errorMessage", "Evaluation has already been submitted and cannot be modified.");
            return "redirect:/applications/" + interview.getApplication().getId();
        }

        // Save evaluation
        interview.setRating(rating);
        interview.setFeedback(feedback.trim());
        interview.setEvaluatedAt(Instant.now());
        interview.setStatus(InterviewStatus.EVALUATED);

        interviewRepository.save(interview);

        // Save ActivityLog
        ActivityLog log = new ActivityLog();
        log.setActor(currentUser);
        log.setActorUsername(currentUser.getUsername());
        log.setEventType(ActivityEventType.EVALUATION_SUBMITTED);
        log.setDescription("Submitted evaluation for interview ID: " + interviewId + " with rating " + rating);
        activityLogRepository.save(log);

        // SPEC FLASH MESSAGE: "Evaluation submitted. Thank you."
        redirect.addFlashAttribute("successMessage", "Evaluation submitted. Thank you.");
        return "redirect:/applications/" + interview.getApplication().getId();
    }
}