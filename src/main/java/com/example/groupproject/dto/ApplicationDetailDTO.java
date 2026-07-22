package com.example.groupproject.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDetailDTO {
    // Thông tin cơ bản ứng viên & đơn ứng tuyển
    private Long applicationId;
    private String candidateName;
    private String candidateEmail;
    private String cvUrl;
    private String status; // APPLIED, SCREENING, INTERVIEW, OFFER, HIRED, REJECTED, WITHDRAWN
    private Instant submissionDate;

    // Dành riêng cho Interviewer (SCR-19)
    private String interviewStatus; // SCHEDULED, EVALUATED
    private Long interviewId;
    private Integer myRating;
    private String myFeedback;

    // Quyền hạn & Danh sách dữ liệu kèm theo
    private boolean canViewInternalNotes;
    private List<InternalNoteDTO> internalNotes = new ArrayList<>();
    private List<EvaluationDTO> evaluations = new ArrayList<>();

    // BỔ SUNG MỚI: Danh sách thông tin lịch phỏng vấn đã gán
    private List<InterviewDTO> interviews = new ArrayList<>();

    // Constructor rỗng
    public ApplicationDetailDTO() {
    }

    // --- Inner DTOs hỗ trợ hiển thị danh sách ---
    public static class InternalNoteDTO {
        private String authorName;
        private String content;
        private Instant createdAt;

        public InternalNoteDTO() {}

        public InternalNoteDTO(String authorName, String content, Instant createdAt) {
            this.authorName = authorName;
            this.content = content;
            this.createdAt = createdAt;
        }

        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public static class EvaluationDTO {
        private String interviewerName;
        private Integer rating;
        private String feedback;
        private Instant evaluatedAt;

        public EvaluationDTO() {}

        public EvaluationDTO(String interviewerName, Integer rating, String feedback, Instant evaluatedAt) {
            this.interviewerName = interviewerName;
            this.rating = rating;
            this.feedback = feedback;
            this.evaluatedAt = evaluatedAt;
        }

        public String getInterviewerName() { return interviewerName; }
        public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        public Instant getEvaluatedAt() { return evaluatedAt; }
        public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    }

    // BỔ SUNG MỚI: Inner DTO chứa chi tiết từng buổi phỏng vấn đã xếp
    public static class InterviewDTO {
        private Long id;
        private String interviewerName;
        private LocalDate interviewDate;
        private LocalTime interviewTime;
        private String locationOrLink;
        private String status;

        public InterviewDTO() {}

        public InterviewDTO(Long id, String interviewerName, LocalDate interviewDate, LocalTime interviewTime, String locationOrLink, String status) {
            this.id = id;
            this.interviewerName = interviewerName;
            this.interviewDate = interviewDate;
            this.interviewTime = interviewTime;
            this.locationOrLink = locationOrLink;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getInterviewerName() { return interviewerName; }
        public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }
        public LocalDate getInterviewDate() { return interviewDate; }
        public void setInterviewDate(LocalDate interviewDate) { this.interviewDate = interviewDate; }
        public LocalTime getInterviewTime() { return interviewTime; }
        public void setInterviewTime(LocalTime interviewTime) { this.interviewTime = interviewTime; }
        public String getLocationOrLink() { return locationOrLink; }
        public void setLocationOrLink(String locationOrLink) { this.locationOrLink = locationOrLink; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // --- Getters & Setters ---
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(Instant submissionDate) { this.submissionDate = submissionDate; }

    public String getInterviewStatus() { return interviewStatus; }
    public void setInterviewStatus(String interviewStatus) { this.interviewStatus = interviewStatus; }

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }

    public Integer getMyRating() { return myRating; }
    public void setMyRating(Integer myRating) { this.myRating = myRating; }

    public String getMyFeedback() { return myFeedback; }
    public void setMyFeedback(String myFeedback) { this.myFeedback = myFeedback; }

    public boolean isCanViewInternalNotes() { return canViewInternalNotes; }
    public void setCanViewInternalNotes(boolean canViewInternalNotes) { this.canViewInternalNotes = canViewInternalNotes; }

    public List<InternalNoteDTO> getInternalNotes() { return internalNotes; }
    public void setInternalNotes(List<InternalNoteDTO> internalNotes) { this.internalNotes = internalNotes; }

    public List<EvaluationDTO> getEvaluations() { return evaluations; }
    public void setEvaluations(List<EvaluationDTO> evaluations) { this.evaluations = evaluations; }

    public List<InterviewDTO> getInterviews() { return interviews; }
    public void setInterviews(List<InterviewDTO> interviews) { this.interviews = interviews; }
}