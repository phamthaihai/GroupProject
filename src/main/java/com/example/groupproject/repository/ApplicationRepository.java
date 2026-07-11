package com.example.groupproject.repository;

import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    long countByStatus(ApplicationStatus status);
    long countByJobId(Integer jobId);
    boolean existsByJobIdAndCandidateId(Integer jobId, Integer candidateId);

    List<Application> findByCandidateIdOrderBySubmittedAtDesc(Integer candidateId);
    List<Application> findByJobIdOrderBySubmittedAtDesc(Integer jobId);
    List<Application> findByJobIdAndStatusOrderBySubmittedAtDesc(Integer jobId, ApplicationStatus status);

    @Query("""
            SELECT COUNT(a) FROM Application a
            WHERE a.status = :status
              AND (:createdById IS NULL OR a.job.createdBy.id = :createdById)
            """)
    long countByStatusScoped(@Param("status") ApplicationStatus status,
                             @Param("createdById") Integer createdById);

    // Dùng cho ApplicationDetailService.java
    @Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.job WHERE a.id = :id")
    Optional<Application> findByIdWithDetails(@Param("id") Integer id);

    // Dùng cho ApplicationDetailService.java
    Optional<Application> findByCandidateId(Integer candidateId);

    // Dùng cho Dashboard/Report
    @Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.job.id = :jobId GROUP BY a.status")
    List<Object[]> countApplicationsByStatusAndJobId(@Param("jobId") Integer jobId);

    // Dùng cho JobManagementService.java
    @Query("SELECT a FROM Application a JOIN FETCH a.candidate WHERE a.job.id = :jobId")
    List<Application> findByJobId(@Param("jobId") Integer jobId);
}