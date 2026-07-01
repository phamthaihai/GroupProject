package com.example.groupproject.repository;

import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for JobPosting data access.
 */
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {

    /**
     * Counts jobs by status for dashboard statistics.
     */
    long countByStatus(JobStatus status);

    /**
     * Counts jobs by status and creator for HR_MANAGER scoped dashboard statistics.
     */
    long countByStatusAndCreatedById(JobStatus status, Integer createdById);

    /**
     * Finds ACTIVE jobs for dashboards, optionally scoped to the creator.
     * Jobs with deadlines appear first; jobs without deadlines appear last.
     */
    @Query("""
            SELECT j FROM JobPosting j
            WHERE j.status = :status
              AND (:createdById IS NULL OR j.createdBy.id = :createdById)
            ORDER BY CASE WHEN j.applicationDeadline IS NULL THEN 1 ELSE 0 END,
                     j.applicationDeadline ASC,
                     j.title ASC
            """)
    List<JobPosting> findActiveJobs(@Param("status") JobStatus status,
                                    @Param("createdById") Integer createdById);

    /**
     * SCR-13 filters are optional and always constrained to ACTIVE postings.
     */
    @Query("""
            SELECT j FROM JobPosting j
            WHERE j.status = :status
              AND (:department IS NULL OR j.department = :department)
              AND (:location IS NULL OR j.location = :location)
            ORDER BY CASE WHEN j.applicationDeadline IS NULL THEN 1 ELSE 0 END,
                     j.applicationDeadline ASC,
                     j.title ASC
            """)
    List<JobPosting> findPublicActiveJobsFiltered(@Param("status") JobStatus status,
                                                  @Param("department") String department,
                                                  @Param("location") String location);

    /**
     * SCR-13 department dropdown values come only from ACTIVE postings.
     */
    @Query("""
            SELECT DISTINCT j.department FROM JobPosting j
            WHERE j.status = :status
            ORDER BY j.department ASC
            """)
    List<String> findDistinctDepartmentsByStatus(@Param("status") JobStatus status);

    /**
     * SCR-13 location dropdown values come only from ACTIVE postings.
     */
    @Query("""
            SELECT DISTINCT j.location FROM JobPosting j
            WHERE j.status = :status
            ORDER BY j.location ASC
            """)
    List<String> findDistinctLocationsByStatus(@Param("status") JobStatus status);
}
