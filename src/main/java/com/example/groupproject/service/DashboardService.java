package com.example.groupproject.service;

import com.example.groupproject.dto.ActiveJobRow;
import com.example.groupproject.dto.AdminDashboardData;
import com.example.groupproject.dto.RecruitmentSummary;
import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.ApplicationStatus;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.entity.view.ActivityLogDisplayView;
import com.example.groupproject.entity.view.JobApplicationCountView;
import com.example.groupproject.repository.ActivityLogDisplayViewRepository;
import com.example.groupproject.repository.ApplicationRepository;
import com.example.groupproject.repository.InterviewRepository;
import com.example.groupproject.repository.JobApplicationCountViewRepository;
import com.example.groupproject.repository.JobPostingRepository;
import com.example.groupproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service tổng hợp dữ liệu cho Admin và HR Dashboard.
 *
 * Scope logic:
 *   - ADMIN: xem toàn bộ (scopeCreatedBy = null)
 *   - HR_MANAGER: chỉ xem data của job do mình tạo (scopeCreatedBy = currentUser.id)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final JobApplicationCountViewRepository jobApplicationCountViewRepository;
    private final UserRepository userRepository;
    private final ActivityLogDisplayViewRepository activityLogDisplayViewRepository;

    /**
     * Lấy recruitment summary (số job active, số applied, số upcoming interview).
     * currentUser == null → global scope (Admin).
     */
    public RecruitmentSummary getRecruitmentSummary(User currentUser) {
        Integer scopeCreatedBy = scopeCreatedBy(currentUser);
        LocalDate today = LocalDate.now();
        LocalDate weekAhead = today.plusDays(7);

        long activeJobs = scopeCreatedBy == null
                ? jobPostingRepository.countByStatus(JobStatus.ACTIVE)
                : jobPostingRepository.countByStatusAndCreatedById(JobStatus.ACTIVE, scopeCreatedBy);

        long applied = applicationRepository.countByStatusScoped(ApplicationStatus.APPLIED, scopeCreatedBy);

        long interviews = interviewRepository.countUpcomingScoped(today, weekAhead, scopeCreatedBy);

        return RecruitmentSummary.builder()
                .activeJobCount(activeJobs)
                .appliedCandidateCount(applied)
                .upcomingInterviewCount(interviews)
                .build();
    }

    /**
     * Lấy danh sách Active Jobs kèm số lượng application — dùng cho HR/Admin dashboard table.
     */
    public List<ActiveJobRow> getActiveJobRows(User currentUser) {
        Integer scopeCreatedBy = scopeCreatedBy(currentUser);
        List<JobPosting> jobs = jobPostingRepository.findActiveJobs(JobStatus.ACTIVE, scopeCreatedBy);

        Map<Integer, JobApplicationCountView> counts = jobApplicationCountViewRepository.findAll().stream()
                .collect(Collectors.toMap(JobApplicationCountView::getJobId, Function.identity()));

        return jobs.stream()
                .map(job -> {
                    JobApplicationCountView count = counts.get(job.getId());
                    long total = count != null ? count.getTotal() : 0L;
                    return ActiveJobRow.builder()
                            .id(job.getId())
                            .title(job.getTitle())
                            .department(job.getDepartment())
                            .applicationCount(total)
                            .deadline(job.getApplicationDeadline())
                            .build();
                })
                .toList();
    }

    /**
     * Lấy toàn bộ dữ liệu cho Admin Dashboard (global scope).
     */
    public AdminDashboardData getAdminDashboardData() {
        Map<UserRole, Long> userCountByRole = new EnumMap<>(UserRole.class);
        for (UserRole role : UserRole.values()) {
            userCountByRole.put(role, userRepository.countByRole(role));
        }

        long lockedCount = userRepository.countByStatus(UserStatus.LOCKED);

        RecruitmentSummary summary = getRecruitmentSummary(null);

        return AdminDashboardData.builder()
                .userCountByRole(userCountByRole)
                .lockedAccountCount(lockedCount)
                .recruitmentSummary(summary)
                .build();
    }

    /** Lấy 10 sự kiện gần nhất cho admin dashboard */
    public List<ActivityLogDisplayView> getRecentActivityEvents() {
        return activityLogDisplayViewRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Xác định scope theo role của currentUser.
     * HR_MANAGER → scope theo id của họ.
     * ADMIN (hoặc null) → null = global scope.
     */
    private Integer scopeCreatedBy(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        if (currentUser.getRole() == UserRole.HR_MANAGER) {
            return currentUser.getId();
        }
        return null;
    }
}
