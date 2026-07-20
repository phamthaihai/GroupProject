package com.example.groupproject.controller;

import com.example.groupproject.dto.InterviewAssignmentDTO;
import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.Interview;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.InterviewRepository;
import com.example.groupproject.repository.UserRepository;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.example.groupproject.repository.ActivityLogRepository;
import com.example.groupproject.entity.ActivityLog;
import com.example.groupproject.entity.enums.ActivityEventType;

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

    // 1. HIỆN FORM ASSIGN
    @GetMapping("/interview/assign/{applicationId}")
    public String showAssignForm(@PathVariable Integer applicationId, Model model) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ID"));

        if (app.getCandidate() != null) app.getCandidate().getFullName();
        if (app.getJob() != null) app.getJob().getTitle();

        List<User> interviewers = userRepository.findByRoleAndStatusOrderByFullNameAsc(UserRole.INTERVIEWER, UserStatus.ACTIVE);
        model.addAttribute("app", app);
        model.addAttribute("interviewers", interviewers);

        InterviewAssignmentDTO dto = new InterviewAssignmentDTO();
        dto.setApplicationId(applicationId);
        model.addAttribute("assignment", dto);

        return "hr/assign-interview";
    }

    // 2. XỬ LÝ LƯU PHỎNG VẤN
    @PostMapping("/interview/assign")
    public String processAssignment(@ModelAttribute InterviewAssignmentDTO dto,
                                    HttpSession session, RedirectAttributes redirect) {
        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) return "redirect:/login";

        LocalDate interviewDate = dto.getInterviewDate();
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
        return "redirect:/applications/" + dto.getApplicationId();
    }

    // 3. HIỆN FORM ĐÁNH GIÁ (SCR-19)
    @GetMapping("/interview/evaluate/{interviewId}")
    public String showEvaluateForm(@PathVariable Integer interviewId, Model model) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Interview ID"));

        if (interview.getApplication() != null) {
            interview.getApplication().getCandidate().getFullName();
            interview.getApplication().getJob().getTitle();
        }

        boolean isEvaluated = (interview.getStatus() == com.example.groupproject.entity.enums.InterviewStatus.EVALUATED);
        model.addAttribute("interview", interview);
        model.addAttribute("isEvaluated", isEvaluated);

        return "hr/evaluate-interview";
    }

    // 4. XỬ LÝ LƯU ĐÁNH GIÁ (Giữ nguyên trạng thái Application để không lỗi DB)
    @PostMapping("/interview/evaluate")
    public String processEvaluation(@RequestParam Integer interviewId,
                                    @RequestParam Short rating,
                                    @RequestParam String feedback,
                                    jakarta.servlet.http.HttpSession session,
                                    RedirectAttributes redirect) {
        User currentUser = authService.getCurrentUser(session);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ID"));

        // Chỉ cập nhật Interview, KHÔNG cập nhật Application status để tránh lỗi constraint
        interview.setRating(rating);
        interview.setFeedback(feedback);
        interview.setEvaluatedAt(java.time.Instant.now());
        interview.setStatus(com.example.groupproject.entity.enums.InterviewStatus.EVALUATED);

        interviewRepository.save(interview);

        if (currentUser != null) {
            ActivityLog log = new ActivityLog();
            log.setActor(currentUser);
            log.setActorUsername(currentUser.getUsername());
            log.setEventType(ActivityEventType.EVALUATION_SUBMITTED);
            log.setDescription("Submitted evaluation for interview ID: " + interviewId + " with rating " + rating);
            activityLogRepository.save(log);
        }

        redirect.addFlashAttribute("message", "Evaluation submitted.");
        return "redirect:/applications/" + interview.getApplication().getId();
    }
}