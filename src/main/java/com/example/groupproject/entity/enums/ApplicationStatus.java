package com.example.groupproject.entity.enums;

/**
 * Trạng thái đơn ứng tuyển.
 * Đồng bộ với CHECK constraint trong bảng applications (talenthub_schema.sql):
 * status IN ('APPLIED','SCREENING','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN')
 */
public enum ApplicationStatus {
    APPLIED,
    SCREENING,
    INTERVIEW,
    EVALUATED,
    OFFER,
    HIRED,
    REJECTED,
    WITHDRAWN
}
