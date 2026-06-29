package com.example.groupproject.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO tổng hợp số liệu recruitment cho dashboard.
 * Dùng cho cả Admin dashboard (global) và HR dashboard (scoped).
 */
@Getter
@Builder
public class RecruitmentSummary {

    /** Số job đang ACTIVE */
    private final long activeJobCount;

    /** Số application đang ở trạng thái APPLIED (chưa xử lý) */
    private final long appliedCandidateCount;

    /** Số interview sắp diễn ra trong 7 ngày tới */
    private final long upcomingInterviewCount;
}
