package com.example.groupproject.entity.enums;

/**
 * Roles trong hệ thống TalentHub.
 * Đồng bộ với CHECK constraint: role IN ('ADMIN','HR_MANAGER','INTERVIEWER','CANDIDATE')
 * trong bảng users (talenthub_schema.sql).
 */
public enum UserRole {
    ADMIN,
    HR_MANAGER,
    INTERVIEWER,
    CANDIDATE
}
