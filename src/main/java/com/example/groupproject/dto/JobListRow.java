package com.example.groupproject.dto;

import com.example.groupproject.entity.enums.JobStatus;
import java.time.LocalDate;

/**
 * DTO đại diện một hàng trong bảng "Jobs" (SCR-10).
 */
public class JobListRow {

    private final Integer id;
    private final String title;
    private final String department;
    private final String location;
    private final JobStatus status;
    private final long applicationCount;
    private final LocalDate deadline;

    public JobListRow(Integer id, String title, String department, String location, JobStatus status, long applicationCount, LocalDate deadline) {
        this.id = id;
        this.title = title;
        this.department = department;
        this.location = location;
        this.status = status;
        this.applicationCount = applicationCount;
        this.deadline = deadline;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDepartment() {
        return department;
    }

    public String getLocation() {
        return location;
    }

    public JobStatus getStatus() {
        return status;
    }

    public long getApplicationCount() {
        return applicationCount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }
}
