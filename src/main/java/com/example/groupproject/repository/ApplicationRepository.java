package com.example.groupproject.repository;

import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository cho entity Application.
 */
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    /** Đếm application theo status — dùng cho admin dashboard (global scope) */
    long countByStatus(ApplicationStatus status);

    /** Đếm số lượng ứng tuyển cho một Job cụ thể */
    long countByJobId(Integer jobId);

    boolean existsByJobIdAndCandidateId(Integer jobId, Integer candidateId);

    List<Application> findByCandidateIdOrderBySubmittedAtDesc(Integer candidateId);

    List<Application> findByJobIdOrderBySubmittedAtDesc(Integer jobId);

    List<Application> findByJobIdAndStatusOrderBySubmittedAtDesc(Integer jobId, ApplicationStatus status);

    /**
     * Đếm application theo status, có thể scope theo người tạo job.
     * Dùng cho HR dashboard: chỉ đếm application thuộc job do HR này tạo.
     *
     * @param status      trạng thái application
     * @param createdById null = tất cả, không null = chỉ application của job do user này tạo
     */
    @Query("""
            SELECT COUNT(a) FROM Application a
            WHERE a.status = :status
              AND (:createdById IS NULL OR a.job.createdBy.id = :createdById)
            """)
    long countByStatusScoped(@Param("status") ApplicationStatus status,
                             @Param("createdById") Integer createdById);
}
