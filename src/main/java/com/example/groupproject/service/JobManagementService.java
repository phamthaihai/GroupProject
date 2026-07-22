package com.example.groupproject.service;

import com.example.groupproject.dto.JobListRow;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobManagementService {

    private final JobPostingRepository jobRepo;
    private final ApplicationRepository appRepo; // Dùng để đếm số lượng application ứng tuyển

    @Autowired
    public JobManagementService(JobPostingRepository jobRepo, ApplicationRepository appRepo) {
        this.jobRepo = jobRepo;
        this.appRepo = appRepo;
    }

    public List<JobListRow> getJobsForList(User currentUser, JobStatus status, String keyword, String department) {
        // 1. Phân quyền: Nếu là HR_MANAGER thì chỉ lấy job của họ, ADMIN thì lấy hết (truyền createdById = null)
        Integer createdById = (currentUser.getRole() == UserRole.ADMIN) ? null : currentUser.getId();

        List<JobPosting> postings = jobRepo.searchJobs(createdById, status, department);

        // 2. Nếu có keyword (từ search) -> lọc chuỗi ở đây bằng Java để tránh lỗi collations SQL
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lower = keyword.toLowerCase(java.util.Locale.ROOT);
            postings = postings.stream()
                    .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase(java.util.Locale.ROOT).contains(lower))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 3. Map sang DTO JobListRow và đếm số lượng apply
        return postings.stream().map(post -> {
            long appCount = appRepo.countByJobId(post.getId());
            return new JobListRow(
                post.getId(), post.getTitle(), post.getDepartment(),
                post.getLocation(), post.getStatus(), appCount, post.getApplicationDeadline()
            );
        }).collect(Collectors.toList());
    }

    public List<String> getDistinctDepartments(User currentUser) {
        Integer createdById = (currentUser.getRole() == UserRole.ADMIN) ? null : currentUser.getId();
        return jobRepo.findDistinctDepartments(createdById);
    }

    public Map<String, Long> getJobCountsByStatus(User currentUser) {
        Integer createdById = (currentUser.getRole() == UserRole.ADMIN) ? null : currentUser.getId();
        Map<String, Long> counts = new HashMap<>();

        long draftCount;
        long activeCount;
        long closedCount;

        if (createdById == null) {
            draftCount = jobRepo.countByStatus(JobStatus.DRAFT);
            activeCount = jobRepo.countByStatus(JobStatus.ACTIVE);
            closedCount = jobRepo.countByStatus(JobStatus.CLOSED);
        } else {
            draftCount = jobRepo.countByStatusAndCreatedById(JobStatus.DRAFT, createdById);
            activeCount = jobRepo.countByStatusAndCreatedById(JobStatus.ACTIVE, createdById);
            closedCount = jobRepo.countByStatusAndCreatedById(JobStatus.CLOSED, createdById);
        }

        counts.put("ALL", draftCount + activeCount + closedCount);
        counts.put("DRAFT", draftCount);
        counts.put("ACTIVE", activeCount);
        counts.put("CLOSED", closedCount);

        return counts;
    }

    public JobPosting getJobById(Integer id, User user) {
        JobPosting job = jobRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found: " + id));
        if (user.getRole() != UserRole.ADMIN && !job.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to view this job posting.");
        }
        return job;
    }

    public JobPosting saveJob(com.example.groupproject.dto.JobFormDTO jobForm, User user) {
        JobPosting job;
        if (jobForm.getId() != null) {
            job = getJobById(jobForm.getId().intValue(), user);
            if (job.getStatus() == JobStatus.CLOSED) {
                throw new IllegalStateException("Closed job postings cannot be edited.");
            }
        } else {
            job = new JobPosting();
            job.setCreatedBy(user);
        }

        job.setTitle(jobForm.getTitle());
        job.setDepartment(jobForm.getDepartment());
        job.setLocation(jobForm.getLocation());
        job.setDescription(jobForm.getDescription());
        job.setRequirements(jobForm.getRequirements());
        job.setSalaryRange(jobForm.getSalaryRange());
        job.setApplicationDeadline(jobForm.getDeadline());
        
        if (jobForm.getStatus() != null && !jobForm.getStatus().isEmpty()) {
            job.setStatus(JobStatus.valueOf(jobForm.getStatus()));
        } else if (job.getId() == null) {
            job.setStatus(JobStatus.DRAFT);
        }

        return jobRepo.save(job);
    }

    // Viết thêm các phương thức thay đổi trạng thái:
    public void publishJob(Integer jobId, User user) {
        JobPosting job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found: " + jobId));

        // Kiểm tra quyền sở hữu hoặc admin
        if (user.getRole() != UserRole.ADMIN && !job.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to publish this job posting.");
        }

        if (job.getStatus() != JobStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT job postings can be published.");
        }

        job.setStatus(JobStatus.ACTIVE);
        jobRepo.save(job);
    }

    public void closeJob(Integer jobId, User user) {
        JobPosting job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found: " + jobId));

        // Kiểm tra quyền sở hữu hoặc admin
        if (user.getRole() != UserRole.ADMIN && !job.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to close this job posting.");
        }

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE job postings can be closed.");
        }

        job.setStatus(JobStatus.CLOSED);
        jobRepo.save(job);
    }

    public void deleteJob(Integer jobId, User user) {
        JobPosting job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job posting not found: " + jobId));

        // Kiểm tra quyền sở hữu hoặc admin
        if (user.getRole() != UserRole.ADMIN && !job.getCreatedBy().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to delete this job posting.");
        }

        if (job.getStatus() != JobStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT job postings can be deleted.");
        }

        long appCount = appRepo.countByJobId(jobId);
        if (appCount > 0) {
            throw new IllegalStateException("Cannot delete job posting because it has active applications.");
        }

        jobRepo.delete(job);
    }

    public List<com.example.groupproject.entity.Application> getApplicationsForJob(Integer jobId, User user) {
        // Kiểm tra quyền xem job trước
        JobPosting job = getJobById(jobId, user);
        return appRepo.findByJobId(job.getId());
    }
}
