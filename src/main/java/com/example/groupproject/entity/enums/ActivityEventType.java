package com.example.groupproject.entity.enums;

/**
 * Các loại sự kiện ghi vào activity_log.
 * Đồng bộ với CHECK constraint trong bảng activity_log (talenthub_schema.sql):
 * event_type IN (
 *   'SIGN_IN_SUCCESS','SIGN_IN_FAILURE','ACCOUNT_CREATED','ACCOUNT_DEACTIVATED',
 *   'ACCOUNT_UNLOCKED','ACCOUNT_LOCKED','APPLICATION_STATUS_CHANGED',
 *   'CV_DOWNLOADED','EVALUATION_SUBMITTED'
 * )
 */
public enum ActivityEventType {
    SIGN_IN_SUCCESS,
    SIGN_IN_FAILURE,
    ACCOUNT_CREATED,
    ACCOUNT_DEACTIVATED,
    ACCOUNT_UNLOCKED,
    ACCOUNT_LOCKED,
    APPLICATION_STATUS_CHANGED,
    CV_DOWNLOADED,
    EVALUATION_SUBMITTED
}
