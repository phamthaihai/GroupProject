package com.example.groupproject.repository;

import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

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
    // Thêm đoạn này vào ApplicationRepository
    @Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.job WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Integer id);
    // Thêm hàm này vào ApplicationRepository
    @Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.job.id = :jobId GROUP BY a.status")
    List<Object[]> countApplicationsByStatusAndJobId(@Param("jobId") Integer jobId);
    // Thêm dòng này để Controller không bị đỏ nữa
    @Query("SELECT a FROM Application a JOIN FETCH a.candidate WHERE a.job.id = :jobId")
    List<Application> findByJobId(@Param("jobId") Integer jobId);
    // Trong file ApplicationRepository.java
    Optional<Application> findByCandidateId(Integer candidateId);
}
