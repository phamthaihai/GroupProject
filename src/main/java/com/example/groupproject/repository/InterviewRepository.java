package com.example.groupproject.repository;

import com.example.groupproject.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * Repository cho entity Interview.
 *
 * Note: Interview entity đã được bổ sung thêm field status, rating, feedback,
 * evaluatedAt, assignedBy, updatedAt theo schema. Repository này dùng JPQL
 * nên tự động tương thích với entity đã sửa.
 */
public interface InterviewRepository extends JpaRepository<Interview, Integer> {

    /**
     * Đếm số interview sắp diễn ra trong khoảng thời gian, có thể scope theo job creator.
     * Dùng cho dashboard stats "upcoming interviews (7 days)".
     *
     * @param from         ngày bắt đầu (today)
     * @param to           ngày kết thúc (today + 7)
     * @param createdById  null = tất cả, không null = chỉ interview của job do user này tạo
     */
    @Query("""
            SELECT COUNT(i) FROM Interview i
            WHERE i.interviewDate BETWEEN :from AND :to
              AND (:createdById IS NULL OR i.application.job.createdBy.id = :createdById)
            """)
    long countUpcomingScoped(@Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             @Param("createdById") Integer createdById);
}
