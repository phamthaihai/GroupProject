package com.example.groupproject.dto;

/**
 * DTO tổng hợp số liệu recruitment cho dashboard.
 */
public class RecruitmentSummary {

    private final long activeJobCount;
    private final long appliedCandidateCount;
    private final long upcomingInterviewCount;

    public RecruitmentSummary(long activeJobCount, long appliedCandidateCount, long upcomingInterviewCount) {
        this.activeJobCount = activeJobCount;
        this.appliedCandidateCount = appliedCandidateCount;
        this.upcomingInterviewCount = upcomingInterviewCount;
    }

    public long getActiveJobCount() {
        return activeJobCount;
    }

    public long getAppliedCandidateCount() {
        return appliedCandidateCount;
    }

    public long getUpcomingInterviewCount() {
        return upcomingInterviewCount;
    }
}
