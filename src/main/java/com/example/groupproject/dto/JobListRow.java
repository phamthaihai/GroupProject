package com.example.groupproject.dto;

import com.example.groupproject.entity.enums.JobStatus;
import java.time.LocalDate;

public class JobListRow {
    private Integer id;
    private String title;
    private String department;
    private String location;
    private JobStatus status;
    private long applicationCount;
    private LocalDate applicationDeadline;

    // Tiến hành tạo Constructor đầy đủ tham số, Getter và Setter
    public JobListRow(Integer id, String title, String department, String location, JobStatus status,
            long applicationCount, LocalDate applicationDeadline) {
        this.id = id;
        this.title = title;
        this.department = department;
        this.location = location;
        this.status = status;
        this.applicationCount = applicationCount;
        this.applicationDeadline = applicationDeadline;
    }
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDepartment() {
        return department;
    }
    public void setDepartment(String department) {
        this.department = department;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public JobStatus getStatus() {
        return status;
    }
    public void setStatus(JobStatus status) {
        this.status = status;
    }
    public long getApplicationCount() {
        return applicationCount;
    }
    public void setApplicationCount(long applicationCount) {
        this.applicationCount = applicationCount;
    }
    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }
    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }
    
}


