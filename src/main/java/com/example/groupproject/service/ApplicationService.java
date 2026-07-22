package com.example.groupproject.service;

import com.example.groupproject.dto.ApplicationDetailDTO;
import com.example.groupproject.dto.ApplicationForm;
import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.Interview;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.ApplicationStatus;
import com.example.groupproject.entity.enums.InterviewStatus;
import com.example.groupproject.repository.ActivityLogRepository;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.InterviewRepository;
import com.example.groupproject.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final FileStorageService fileStorageService;
    private final ActivityLogRepository activityLogRepository;
    private final InterviewRepository interviewRepository; // Inject thêm InterviewRepository

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobPostingRepository jobPostingRepository,
                              FileStorageService fileStorageService,
                              ActivityLogRepository activityLogRepository,
                              InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.fileStorageService = fileStorageService;
        this.activityLogRepository = activityLogRepository;
        this.interviewRepository = interviewRepository;
    }

    public boolean hasApplied(Integer jobId, Integer candidateId) {
        return applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId);
    }

    @Transactional
    public Application applyToJob(Integer jobId, User candidate, ApplicationForm form) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found"));

        if (job.getStatus() != com.example.groupproject.entity.enums.JobStatus.ACTIVE) {
            throw new IllegalStateException("This position is no longer accepting applications.");
        }

        if (hasApplied(jobId, candidate.getId())) {
            throw new IllegalStateException("You have already applied for this position.");
        }

        String storagePath = fileStorageService.storeFile(form.getCvFile());
        String originalFilename = form.getCvFile().getOriginalFilename();

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter(form.getCoverLetter() != null ? form.getCoverLetter().trim() : null);
        application.setCvFilename(originalFilename);
        application.setCvStoragePath(storagePath);
        application.setStatus(ApplicationStatus.APPLIED);

        Application savedApp = applicationRepository.save(application);

        com.example.groupproject.entity.ActivityLog log = new com.example.groupproject.entity.ActivityLog();
        log.setActor(candidate);
        log.setActorUsername(candidate.getUsername());
        log.setEventType(com.example.groupproject.entity.enums.ActivityEventType.APPLICATION_STATUS_CHANGED);
        log.setDescription("Candidate " + candidate.getUsername() + " applied for job: " + job.getTitle());
        activityLogRepository.save(log);

        return savedApp;
    }

    public List<Application> getApplicationsByCandidate(Integer candidateId) {
        return applicationRepository.findByCandidateIdOrderBySubmittedAtDesc(candidateId);
    }

    @Transactional
    public void withdrawApplication(Integer applicationId, User candidate) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (!app.getCandidate().getId().equals(candidate.getId())) {
            throw new IllegalStateException("Access denied");
        }

        if (app.getStatus() != ApplicationStatus.APPLIED && app.getStatus() != ApplicationStatus.SCREENING) {
            throw new IllegalStateException("Only applications in APPLIED or SCREENING stage can be withdrawn.");
        }

        app.setStatus(ApplicationStatus.WITHDRAWN);
        app.setStatusChangedAt(java.time.Instant.now());
        applicationRepository.save(app);

        com.example.groupproject.entity.ActivityLog log = new com.example.groupproject.entity.ActivityLog();
        log.setActor(candidate);
        log.setActorUsername(candidate.getUsername());
        log.setEventType(com.example.groupproject.entity.enums.ActivityEventType.APPLICATION_STATUS_CHANGED);
        log.setDescription("Candidate " + candidate.getUsername() + " withdrew application for job: " + app.getJob().getTitle());
        activityLogRepository.save(log);
    }

    public List<Application> getApplicationsForJob(Integer jobId, String status, User currentUser) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found"));

        if (currentUser.getRole() != com.example.groupproject.entity.enums.UserRole.ADMIN) {
            if (currentUser.getRole() != com.example.groupproject.entity.enums.UserRole.HR_MANAGER
                    || !job.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        if ("ALL".equalsIgnoreCase(status)) {
            return applicationRepository.findByJobId(jobId);
        } else {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            return applicationRepository.findByJobIdAndStatusOrderBySubmittedAtDesc(jobId, appStatus);
        }
    }

    public java.util.Map<String, Long> getApplicationCountsByStage(Integer jobId) {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        List<Application> allApps = applicationRepository.findByJobId(jobId);

        counts.put("ALL", (long) allApps.size());
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = allApps.stream().filter(a -> a.getStatus() == status).count();
            counts.put(status.name(), count);
        }
        return counts;
    }

    // =========================================================================
    // BỔ SUNG MỚI: LẤY DỮ LIỆU CHI TIẾT ĐƠN ỨNG TUYỂN (SCR-17 / SCR-18)
    // =========================================================================
    public ApplicationDetailDTO getApplicationDetail(Integer applicationId, User currentUser) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        ApplicationDetailDTO dto = new ApplicationDetailDTO();
        dto.setApplicationId(app.getId() != null ? app.getId().longValue() : null);
        dto.setCandidateName(app.getCandidate() != null ? app.getCandidate().getFullName() : "N/A");
        dto.setCandidateEmail(app.getCandidate() != null ? app.getCandidate().getEmail() : "N/A");
        dto.setCvUrl(app.getCvStoragePath());
        dto.setStatus(app.getStatus() != null ? app.getStatus().name() : "");
        dto.setSubmissionDate(app.getSubmittedAt());

        // 1. Lấy danh sách Lịch phỏng vấn (Interviews) thuộc đơn này
        List<Interview> interviewList = interviewRepository.findByApplicationId(applicationId);
        List<ApplicationDetailDTO.InterviewDTO> interviewDTOs = new ArrayList<>();
        List<ApplicationDetailDTO.EvaluationDTO> evaluationDTOs = new ArrayList<>();

        for (Interview itv : interviewList) {
            // Map vào danh sách Lịch phỏng vấn cho HR/Admin xem
            ApplicationDetailDTO.InterviewDTO itvDto = new ApplicationDetailDTO.InterviewDTO();
            itvDto.setId(itv.getId() != null ? itv.getId().longValue() : null);
            itvDto.setInterviewerName(itv.getInterviewer() != null ? itv.getInterviewer().getFullName() : "N/A");
            itvDto.setInterviewDate(itv.getInterviewDate());
            itvDto.setInterviewTime(itv.getInterviewTime());
            itvDto.setLocationOrLink(itv.getLocationOrLink());
            itvDto.setStatus(itv.getStatus() != null ? itv.getStatus().name() : "SCHEDULED");
            interviewDTOs.add(itvDto);

            // Nếu buổi phỏng vấn đã đánh giá xong, map vào danh sách Evaluation
            if (itv.getStatus() == InterviewStatus.EVALUATED) {
                ApplicationDetailDTO.EvaluationDTO evalDto = new ApplicationDetailDTO.EvaluationDTO();
                evalDto.setInterviewerName(itv.getInterviewer() != null ? itv.getInterviewer().getFullName() : "N/A");
                evalDto.setRating(itv.getRating() != null ? itv.getRating().intValue() : 0);
                evalDto.setFeedback(itv.getFeedback());
                evalDto.setEvaluatedAt(itv.getEvaluatedAt());
                evaluationDTOs.add(evalDto);
            }

            // Nếu user đăng nhập là Interviewer của buổi phỏng vấn này -> Set thông tin riêng
            if (currentUser != null && itv.getInterviewer() != null
                    && itv.getInterviewer().getId().equals(currentUser.getId())) {
                dto.setInterviewId(itv.getId() != null ? itv.getId().longValue() : null);
                dto.setInterviewStatus(itv.getStatus() != null ? itv.getStatus().name() : "SCHEDULED");
                dto.setMyRating(itv.getRating() != null ? itv.getRating().intValue() : null);
                dto.setMyFeedback(itv.getFeedback());
            }
        }

        dto.setInterviews(interviewDTOs);
        dto.setEvaluations(evaluationDTOs);

        return dto;
    }
}