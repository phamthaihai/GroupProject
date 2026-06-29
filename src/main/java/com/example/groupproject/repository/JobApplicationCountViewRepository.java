package com.example.groupproject.repository;

import com.example.groupproject.entity.view.JobApplicationCountView;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho VIEW v_job_application_counts.
 * Dùng để lấy số lượng application theo trạng thái cho từng job.
 */
public interface JobApplicationCountViewRepository extends JpaRepository<JobApplicationCountView, Integer> {
}
