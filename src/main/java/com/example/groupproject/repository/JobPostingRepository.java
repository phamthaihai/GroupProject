package com.example.groupproject.repository;

import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository cho entity JobPosting.
 */
public interface JobPostingRepository extends JpaRepository<JobPosting, Integer> {

    /** Đếm job theo status — dùng cho dashboard stats */
    long countByStatus(JobStatus status);

    /**
     * Đếm job theo status và người tạo — dùng cho HR dashboard (scope theo HR_MANAGER).
     * createdById là id của User (join createdBy.id).
     */
    long countByStatusAndCreatedById(JobStatus status, Integer createdById);

    /**
     * Lấy danh sách job ACTIVE, có thể scope theo người tạo (HR_MANAGER).
     * Sắp xếp: job có deadline lên trước, NULL deadline xuống cuối, rồi theo title.
     *
     * @param status      thường là JobStatus.ACTIVE
     * @param createdById null = tất cả job (ADMIN view), không null = chỉ job của HR này
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


    // Truy vấn danh sách công việc lọc theo điều kiện và scope người tạo
    @Query("""
            SELECT j FROM JobPosting j
            WHERE (:createdById IS NULL OR j.createdBy.id = :createdById)
              AND (:status IS NULL OR j.status = :status)
              AND (:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:department IS NULL OR j.department = :department)
            ORDER BY j.updatedAt DESC
            """)
    List<JobPosting> findJobsForList(@Param("createdById") Integer createdById,
                                     @Param("status") JobStatus status,
                                     @Param("keyword") String keyword,
                                     @Param("department") String department);

    // 2. Lấy danh sách các phòng ban không trùng lặp để đưa vào dropdown filter
    @Query("""
            SELECT DISTINCT j.department FROM JobPosting j
            WHERE (:createdById IS NULL OR j.createdBy.id = :createdById)
            ORDER BY j.department ASC
            """)
    List<String> findDistinctDepartments(@Param("createdById") Integer createdById);
}
