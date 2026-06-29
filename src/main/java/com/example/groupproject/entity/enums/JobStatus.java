package com.example.groupproject.entity.enums;

/**
 * Trạng thái tin tuyển dụng.
 * Đồng bộ với CHECK constraint trong bảng job_postings (talenthub_schema.sql):
 * status IN ('DRAFT','ACTIVE','CLOSED')
 */
public enum JobStatus {
    DRAFT,
    ACTIVE,
    CLOSED
}
