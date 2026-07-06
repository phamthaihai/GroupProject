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

    @Autowired
    private JobPostingRepository jobRepo;

    @Autowired
    private ApplicationRepository appRepo; // Dùng để đếm số lượng application ứng tuyển

    public List<JobListRow> getJobsForList(User currentUser, JobStatus status, String keyword, String department) {
        // 1. Phân quyền: Nếu là HR_MANAGER thì chỉ lấy job của họ, ADMIN thì lấy hết (truyền createdById = null)
        Integer createdById = (currentUser.getRole() == UserRole.ADMIN) ? null : currentUser.getId();

        List<JobPosting> postings = jobRepo.findJobsForList(createdById, status, keyword, department);

        // 2. Map sang DTO JobListRow và đếm số lượng apply
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

        // Nhớ check điều kiện: Phải chưa có ai ứng tuyển (applicationCount == 0) mới được xóa!
        long appCount = appRepo.countByJobId(jobId);
        if (appCount > 0) {
            throw new IllegalStateException("Cannot delete job posting because it has active applications.");
        }

        jobRepo.delete(job);
    }
}
