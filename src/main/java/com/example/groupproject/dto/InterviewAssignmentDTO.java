package com.example.groupproject.dto;

import java.time.LocalDate;

public class InterviewAssignmentDTO {
    private Integer applicationId;
    private Integer interviewerId;
    private LocalDate interviewDate;
    private String interviewTime;
    private String locationOrLink;

    // Bổ sung Context Header
    private String candidateName;
    private String jobTitle;

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

    public Integer getInterviewerId() { return interviewerId; }
    public void setInterviewerId(Integer interviewerId) { this.interviewerId = interviewerId; }

    public LocalDate getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDate interviewDate) { this.interviewDate = interviewDate; }

    public String getInterviewTime() { return interviewTime; }
    public void setInterviewTime(String interviewTime) { this.interviewTime = interviewTime; }

    public String getLocationOrLink() { return locationOrLink; }
    public void setLocationOrLink(String locationOrLink) { this.locationOrLink = locationOrLink; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
}