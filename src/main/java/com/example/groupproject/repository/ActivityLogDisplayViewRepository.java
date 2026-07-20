package com.example.groupproject.repository;

import com.example.groupproject.entity.view.ActivityLogDisplayView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.groupproject.entity.enums.ActivityEventType;
import java.time.Instant;
import java.util.List;

/**
 * Repository cho VIEW v_activity_log_display.
 * Dùng để lấy 10 sự kiện gần nhất cho admin dashboard.
 */
public interface ActivityLogDisplayViewRepository extends JpaRepository<ActivityLogDisplayView, Long> {

    /** Lấy 10 log sự kiện mới nhất — dùng cho admin dashboard */
    List<ActivityLogDisplayView> findTop10ByOrderByCreatedAtDesc();

    @Query("""
        SELECT v FROM ActivityLogDisplayView v
        WHERE (cast(:eventType as string) IS NULL OR v.eventType = :eventType)
          AND (cast(:search as string) IS NULL OR :search = '' 
               OR LOWER(v.actorUsername) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
               OR LOWER(v.actorDisplayName) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
          AND (cast(:dateFrom as timestamp) IS NULL OR v.createdAt >= :dateFrom)
          AND (cast(:dateTo as timestamp) IS NULL OR v.createdAt <= :dateTo)
        """)
    Page<ActivityLogDisplayView> searchLogs(
        @Param("eventType") ActivityEventType eventType,
        @Param("search") String search,
        @Param("dateFrom") Instant dateFrom,
        @Param("dateTo") Instant dateTo,
        Pageable pageable
    );
}
