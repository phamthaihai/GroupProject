package com.example.groupproject.dto;

import com.example.groupproject.entity.enums.UserRole;

import java.util.Map;

/**
 * DTO tổng hợp dữ liệu cho Admin Dashboard.
 */
public class AdminDashboardData {

    private final Map<UserRole, Long> userCountByRole;
    private final long lockedAccountCount;
    private final RecruitmentSummary recruitmentSummary;

    public AdminDashboardData(Map<UserRole, Long> userCountByRole,
                              long lockedAccountCount,
                              RecruitmentSummary recruitmentSummary) {
        this.userCountByRole = userCountByRole;
        this.lockedAccountCount = lockedAccountCount;
        this.recruitmentSummary = recruitmentSummary;
    }

    public Map<UserRole, Long> getUserCountByRole() {
        return userCountByRole;
    }

    public long getLockedAccountCount() {
        return lockedAccountCount;
    }

    public RecruitmentSummary getRecruitmentSummary() {
        return recruitmentSummary;
    }
}
