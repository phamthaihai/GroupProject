package com.example.groupproject.dto;

import java.util.Date;
import java.util.List;

public class ApplicationDetailDTO {
    private Long applicationId;
    private String candidateName;
    private String candidateEmail;
    private Date submittedDate;
    private String status;
    private boolean canViewInternalNotes;
    private List<String> internalNotes; // Chỉ nạp nếu có quyền

    public ApplicationDetailDTO(Long applicationId, String candidateName, String candidateEmail, Date submittedDate, String status, boolean canViewInternalNotes) {
        this.applicationId = applicationId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.submittedDate = submittedDate;
        this.status = status;
        this.canViewInternalNotes = canViewInternalNotes;
    }

    // Getters and Setters
    public Long getApplicationId() { return applicationId; }
    public String getCandidateName() { return candidateName; }
    public String getCandidateEmail() { return candidateEmail; }
    public Date getSubmittedDate() { return submittedDate; }
    public String getStatus() { return status; }
    public boolean isCanViewInternalNotes() { return canViewInternalNotes; }
    public List<String> getInternalNotes() { return internalNotes; }
    public void setInternalNotes(List<String> internalNotes) { this.internalNotes = internalNotes; }
}