package com.example.groupproject.repository;

import com.example.groupproject.entity.view.ActivityLogDisplayView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository cho VIEW v_activity_log_display.
 * Dùng để lấy 10 sự kiện gần nhất cho admin dashboard.
 */
public interface ActivityLogDisplayViewRepository extends JpaRepository<ActivityLogDisplayView, Long> {

    /** Lấy 10 log sự kiện mới nhất — dùng cho admin dashboard */
    List<ActivityLogDisplayView> findTop10ByOrderByCreatedAtDesc();
}
