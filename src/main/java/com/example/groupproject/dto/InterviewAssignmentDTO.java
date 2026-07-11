package com.example.groupproject.dto;

import java.time.LocalDate;

public class InterviewAssignmentDTO {
    private Integer applicationId;
    private Integer interviewerId;
    private LocalDate interviewDate;
    private String interviewTime;
    private String locationOrLink;

    // NHỚ CÓ CÁC HÀM GETTER VÀ SETTER
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
}