package com.example.groupproject.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * DTO đại diện một hàng trong bảng "Active Jobs" trên HR/Admin dashboard.
 * Chứa thông tin tổng hợp: job info + số lượng application.
 */
@Getter
@Builder
public class ActiveJobRow {

    private final Integer id;
    private final String title;
    private final String department;
    private final long applicationCount;
    private final LocalDate deadline;
}
