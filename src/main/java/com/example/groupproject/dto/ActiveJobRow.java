package com.example.groupproject.dto;

import java.time.LocalDate;

/**
 * DTO đại diện một hàng trong bảng "Active Jobs" trên HR/Admin dashboard.
 */
public class ActiveJobRow {

    private final Integer id;
    private final String title;
    private final String department;
    private final long applicationCount;
    private final LocalDate deadline;

    public ActiveJobRow(Integer id, String title, String department, long applicationCount, LocalDate deadline) {
        this.id = id;
        this.title = title;
        this.department = department;
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

    public long getApplicationCount() {
        return applicationCount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }
}
