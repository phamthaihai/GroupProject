package com.example.groupproject.service;

import com.example.groupproject.dto.ApplicationDetailDTO;
import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.Interview;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.ApplicationStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.InterviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ApplicationDetailService {

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    public ApplicationDetailService(ApplicationRepository applicationRepository,
                                    InterviewRepository interviewRepository) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
    }

    @Transactional(readOnly = true)
    public ApplicationDetailDTO getApplicationDetail(Long applicationId, User currentUser) {
        // 1. Tải ứng tuyển
        Application app = applicationRepository.findByIdWithDetails(applicationId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ứng tuyển với ID: " + applicationId));

        // 2. BẢO MẬT (Security Check): Interviewer chỉ xem được đơn được gán cho mình
        if (currentUser.getRole() == UserRole.INTERVIEWER) {
            boolean isAssigned = interviewRepository.existsByApplicationIdAndInterviewerId(
                    app.getId(), currentUser.getId());

            if (!isAssigned) {
                throw new SecurityException("Bạn không có quyền truy cập đơn ứng tuyển này (Access Denied).");
            }
        }

        // 3. Khởi tạo DTO và map các trường thông tin cơ bản
        ApplicationDetailDTO dto = new ApplicationDetailDTO();
        dto.setApplicationId(app.getId().longValue());

        if (app.getCandidate() != null) {
            dto.setCandidateName(app.getCandidate().getFullName());
            dto.setCandidateEmail(app.getCandidate().getEmail());
        }

        dto.setStatus(app.getStatus().name());
        dto.setSubmissionDate(app.getSubmittedAt());
        dto.setCvUrl(app.getCvStoragePath());

        // 4. Quyền xem Internal Notes
        boolean canViewNotes = (currentUser.getRole() == UserRole.ADMIN ||
                currentUser.getRole() == UserRole.HR_MANAGER);
        dto.setCanViewInternalNotes(canViewNotes);

        // 5. Nạp danh sách Internal Notes
        if (canViewNotes) {
            dto.setInternalNotes(Collections.emptyList());
        }

        // 6. Nạp thông tin phỏng vấn & Đánh giá (Evaluations)
        List<Interview> interviews = interviewRepository.findByApplicationId(app.getId());
        if (!interviews.isEmpty()) {
            List<ApplicationDetailDTO.InterviewDTO> interviewDTOList = new ArrayList<>();
            List<ApplicationDetailDTO.EvaluationDTO> evaluationDTOList = new ArrayList<>();

            for (Interview interview : interviews) {
                // Map từng buổi phỏng vấn vào danh sách Lịch phỏng vấn cho HR/Admin xem
                ApplicationDetailDTO.InterviewDTO itvDTO = new ApplicationDetailDTO.InterviewDTO();
                itvDTO.setId(interview.getId() != null ? interview.getId().longValue() : null);
                itvDTO.setInterviewerName(interview.getInterviewer() != null ? interview.getInterviewer().getFullName() : "N/A");
                itvDTO.setInterviewDate(interview.getInterviewDate());
                itvDTO.setInterviewTime(interview.getInterviewTime());
                itvDTO.setLocationOrLink(interview.getLocationOrLink());
                itvDTO.setStatus(interview.getStatus() != null ? interview.getStatus().name() : "SCHEDULED");
                interviewDTOList.add(itvDTO);

                // Nếu là Interviewer chính buổi này -> Nạp dữ liệu riêng
                if (currentUser.getRole() == UserRole.INTERVIEWER
                        && interview.getInterviewer() != null
                        && interview.getInterviewer().getId().equals(currentUser.getId())) {
                    dto.setInterviewId(interview.getId().longValue());
                    if (interview.getStatus() != null) {
                        dto.setInterviewStatus(interview.getStatus().name());
                    }
                    dto.setMyRating(interview.getRating() != null ? interview.getRating().intValue() : null);
                    dto.setMyFeedback(interview.getFeedback());
                }

                // Nếu HR/Admin xem và đã có bài đánh giá (rating != null) -> Nạp vào Evaluations
                if (canViewNotes && interview.getRating() != null) {
                    ApplicationDetailDTO.EvaluationDTO evalDTO = new ApplicationDetailDTO.EvaluationDTO(
                            interview.getInterviewer() != null ? interview.getInterviewer().getFullName() : "Interviewer",
                            interview.getRating() != null ? interview.getRating().intValue() : 0,
                            interview.getFeedback(),
                            interview.getEvaluatedAt()
                    );
                    evaluationDTOList.add(evalDTO);
                }
            }

            // SET DANH SÁCH INTERVIEW DTO VÀ EVALUATION DTO VÀO CONTAINER DTO
            dto.setInterviews(interviewDTOList);
            dto.setEvaluations(evaluationDTOList);
        }

        return dto;
    }

    // Tiến trình chuyển trạng thái đơn ứng tuyển (Move to Screening / Interview / Offer)
    @Transactional
    public void advanceApplicationStatus(Long applicationId, User currentUser) {
        Application app = applicationRepository.findById(applicationId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ứng tuyển với ID: " + applicationId));

        if (app.getStatus() == ApplicationStatus.APPLIED) {
            app.setStatus(ApplicationStatus.SCREENING);
        } else if (app.getStatus() == ApplicationStatus.SCREENING) {
            app.setStatus(ApplicationStatus.INTERVIEW);
        } else if (app.getStatus() == ApplicationStatus.INTERVIEW) {
            app.setStatus(ApplicationStatus.OFFER);
        } else if (app.getStatus() == ApplicationStatus.OFFER) {
            app.setStatus(ApplicationStatus.HIRED);
        }

        app.setStatusChangedAt(Instant.now());
        applicationRepository.save(app);
    }

    // Thêm ghi chú nội bộ
    @Transactional
    public void addNote(Long applicationId, String content, User author) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung ghi chú không được để trống.");
        }

        Application app = applicationRepository.findById(applicationId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ứng tuyển."));
    }

    public Long findAppIdByUserId(Long userId) {
        return applicationRepository.findByCandidateId(userId.intValue())
                .map(app -> app.getId().longValue())
                .orElse(null);
    }
}