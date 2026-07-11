package com.example.groupproject.service;

import com.example.groupproject.entity.JobPosting;
import com.example.groupproject.entity.enums.JobStatus;
import com.example.groupproject.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Domain service for job-posting use cases.
 */
@Service
@Transactional(readOnly = true)
public class JobService {

    private final JobPostingRepository jobPostingRepository;

    public JobService(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    /**
     * Builds all data SCR-13 needs while validating filters against ACTIVE-only options.
     */
    public PublicJobListData getPublicJobList(String departmentFilter, String locationFilter) {
        List<String> departments = jobPostingRepository.findDistinctDepartmentsByStatus(JobStatus.ACTIVE);
        List<String> locations = jobPostingRepository.findDistinctLocationsByStatus(JobStatus.ACTIVE);

        String selectedDepartment = normalizeFilter(departmentFilter, departments);
        String selectedLocation = normalizeFilter(locationFilter, locations);

        List<JobPosting> jobs = jobPostingRepository.findPublicActiveJobsFiltered(
                JobStatus.ACTIVE,
                selectedDepartment,
                selectedLocation
        );

        return new PublicJobListData(
                jobs,
                departments,
                locations,
                selectedDepartment,
                selectedLocation,
                !departments.isEmpty() || !locations.isEmpty()
        );
    }

    /**
     * Blank or unknown filter values are ignored so request parameters cannot expose non-active data.
     */
    private String normalizeFilter(String value, List<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return allowedValues.contains(trimmed) ? trimmed : null;
    }

    public record PublicJobListData(
            List<JobPosting> jobs,
            List<String> departments,
            List<String> locations,
            String selectedDepartment,
            String selectedLocation,
            boolean hasActivePostings
    ) {
        public boolean hasSelectedFilter() {
            return selectedDepartment != null || selectedLocation != null;
        }
    }

    public JobPosting getJobById(Integer id) {
        return jobPostingRepository.findById(id).orElse(null);
    }
}
