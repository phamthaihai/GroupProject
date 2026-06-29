package com.example.groupproject.dto;

import com.example.groupproject.entity.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * DTO tổng hợp dữ liệu cho Admin Dashboard.
 * Bao gồm: số user theo từng role, số tài khoản bị khóa, và recruitment summary.
 */
@Getter
@Builder
public class AdminDashboardData {

    /** Số lượng user theo từng role (ADMIN, HR_MANAGER, INTERVIEWER, CANDIDATE) */
    private final Map<UserRole, Long> userCountByRole;

    /** Số tài khoản đang bị LOCKED */
    private final long lockedAccountCount;

    /** Recruitment summary (global scope — tất cả job/application/interview) */
    private final RecruitmentSummary recruitmentSummary;
}
