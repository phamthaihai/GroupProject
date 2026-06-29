package com.example.groupproject.entity.enums;

/**
 * Trạng thái tài khoản người dùng.
 * Đồng bộ với CHECK constraint: status IN ('ACTIVE','LOCKED','INACTIVE')
 * trong bảng users (talenthub_schema.sql).
 */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    INACTIVE
}
