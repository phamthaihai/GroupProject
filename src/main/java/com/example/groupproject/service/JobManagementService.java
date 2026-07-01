package com.example.groupproject.service;

import com.example.groupproject.dto.JobListRow;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.view.JobApplicationCountView;
import com.example.groupproject.repository.JobApplicationCountViewRepository;
import com.example.groupproject.repository.JobPostingRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service cho Job Management module.
 */
@Service
@Transactional(readOnly = true)
public class JobManagementService {

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationCountViewRepository jobApplicationCountViewRepository;

    public JobManagementService(JobPostingRepository jobPostingRepository,
                                JobApplicationCountViewRepository jobApplicationCountViewRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.jobApplicationCountViewRepository = jobApplicationCountViewRepository;
    }

    private Specification<JobPosting> buildSpec(Integer scopeCreatedBy, JobStatus status, String keyword, String department) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (scopeCreatedBy != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), scopeCreatedBy));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"));
            }
            if (department != null) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<JobListRow> getJobsForList(User currentUser, JobStatus status, String keyword, String department) {
        Integer scopeCreatedBy = scopeCreatedBy(currentUser);
        
        // Handle empty strings as null
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        if (department != null && department.trim().isEmpty()) department = null;
        
        List<JobPosting> jobs = jobPostingRepository.findAll(
                buildSpec(scopeCreatedBy, status, keyword, department),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Map<Integer, JobApplicationCountView> counts = jobApplicationCountViewRepository.findAll().stream()
                .collect(Collectors.toMap(JobApplicationCountView::getJobId, Function.identity()));

        return jobs.stream()
                .map(job -> {
                    JobApplicationCountView count = counts.get(job.getId());
                    long total = count != null ? count.getTotal() : 0L;
                    return new JobListRow(
                            job.getId(),
                            job.getTitle(),
                            job.getDepartment(),
                            job.getLocation(),
                            job.getStatus(),
                            total,
                            job.getApplicationDeadline()
                    );
                })
                .toList();
    }

    public List<String> getDistinctDepartments(User currentUser) {
        return jobPostingRepository.findDistinctDepartments(scopeCreatedBy(currentUser));
    }

    public Map<String, Long> getJobCountsByStatus(User currentUser) {
        Integer scopeCreatedBy = scopeCreatedBy(currentUser);
        Map<String, Long> counts = new HashMap<>();
        
        long total = jobPostingRepository.count(buildSpec(scopeCreatedBy, null, null, null));
        counts.put("ALL", total);
        
        long draft = jobPostingRepository.count(buildSpec(scopeCreatedBy, JobStatus.DRAFT, null, null));
        counts.put("DRAFT", draft);
        
        long active = jobPostingRepository.count(buildSpec(scopeCreatedBy, JobStatus.ACTIVE, null, null));
        counts.put("ACTIVE", active);
        
        long closed = jobPostingRepository.count(buildSpec(scopeCreatedBy, JobStatus.CLOSED, null, null));
        counts.put("CLOSED", closed);
        
        return counts;
    }

    private Integer scopeCreatedBy(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        if (currentUser.getRole() == UserRole.HR_MANAGER) {
            return currentUser.getId();
        }
        return null; // Admin sees all
    }

    public JobPosting getJobById(Integer id, User currentUser) {
        JobPosting job = jobPostingRepository.findById(id).orElse(null);
        if (job == null) return null;
        Integer scope = scopeCreatedBy(currentUser);
        if (scope != null && !job.getCreatedBy().getId().equals(scope)) {
            return null; // access denied
        }
        return job;
    }

    @Transactional
    public Integer createJobFromDto(com.example.groupproject.dto.JobFormDto dto, User currentUser) {
        JobPosting job = new JobPosting();
        job.setTitle(dto.getTitle());
        job.setDepartment(dto.getDepartment());
        job.setLocation(dto.getLocation());
        job.setDescription(dto.getDescription());
        job.setRequirements(dto.getRequirements());
        job.setSalaryRange(dto.getSalaryRange());
        job.setApplicationDeadline(dto.getApplicationDeadline());
        job.setStatus(JobStatus.DRAFT);
        job.setCreatedBy(currentUser);
        job = jobPostingRepository.save(job);
        return job.getId();
    }

    @Transactional
    public void updateJobFromDto(Integer id, com.example.groupproject.dto.JobFormDto dto, User currentUser) {
        JobPosting job = getJobById(id, currentUser);
        if (job == null) throw new IllegalArgumentException("Job not found or access denied");
        if (job.getStatus() == JobStatus.CLOSED) throw new IllegalStateException("Cannot edit closed job");

        job.setTitle(dto.getTitle());
        job.setDepartment(dto.getDepartment());
        job.setLocation(dto.getLocation());
        job.setDescription(dto.getDescription());
        job.setRequirements(dto.getRequirements());
        job.setSalaryRange(dto.getSalaryRange());
        job.setApplicationDeadline(dto.getApplicationDeadline());
        jobPostingRepository.save(job);
    }

    @Transactional
    public void publishJob(Integer id, User currentUser) {
        JobPosting job = getJobById(id, currentUser);
        if (job != null && job.getStatus() == JobStatus.DRAFT) {
            job.setStatus(JobStatus.ACTIVE);
            jobPostingRepository.save(job);
        }
    }
    @Transactional
    public void deleteJob(Integer id, User currentUser) {
        JobPosting job = getJobById(id, currentUser);
        if (job != null && job.getStatus() == JobStatus.DRAFT) {
            long count = jobApplicationCountViewRepository.findById(id).map(JobApplicationCountView::getTotal).orElse(0L);
            if (count == 0) {
                jobPostingRepository.delete(job);
            } else {
                throw new IllegalStateException("Cannot delete job with existing applications.");
            }
        }
    }

    @Transactional
    public void closeJob(Integer id, User currentUser) {
        JobPosting job = getJobById(id, currentUser);
        if (job != null && job.getStatus() == JobStatus.ACTIVE) {
            job.setStatus(JobStatus.CLOSED);
            jobPostingRepository.save(job);
        }
    }
}
