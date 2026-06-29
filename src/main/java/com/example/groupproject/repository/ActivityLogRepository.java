package com.example.groupproject.repository;

import com.example.groupproject.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho entity ActivityLog.
 * Dùng để ghi log sự kiện vào bảng activity_log.
 * Read được thực hiện qua ActivityLogDisplayViewRepository (dùng VIEW).
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
