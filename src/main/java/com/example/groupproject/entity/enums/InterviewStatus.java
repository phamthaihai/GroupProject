package com.example.groupproject.entity.enums;

/**
 * Trạng thái buổi phỏng vấn.
 * Đồng bộ với CHECK constraint trong bảng interviews (talenthub_schema.sql):
 * status IN ('SCHEDULED','EVALUATED')
 *
 * Logic bổ sung từ schema:
 * - SCHEDULED: chưa đánh giá, rating/feedback/evaluated_at là NULL
 * - EVALUATED: đã đánh giá, rating/feedback/evaluated_at bắt buộc NOT NULL
 *   (được enforce bởi chk_evaluated_fields CHECK constraint)
 */
public enum InterviewStatus {
    SCHEDULED,
    EVALUATED
}
