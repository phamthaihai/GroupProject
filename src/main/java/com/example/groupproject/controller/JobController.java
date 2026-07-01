package com.example.groupproject.controller;

import com.example.groupproject.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for job-posting management and public job browsing.
 */
@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * SCR-13: public active job listing with optional department/location filters.
     */
    @GetMapping
    public String publicJobList(@RequestParam(required = false) String department,
                                @RequestParam(required = false) String location,
                                Model model) {
        JobService.PublicJobListData data = jobService.getPublicJobList(department, location);
        model.addAttribute("jobs", data.jobs());
        model.addAttribute("departments", data.departments());
        model.addAttribute("locations", data.locations());
        model.addAttribute("selectedDepartment", data.selectedDepartment());
        model.addAttribute("selectedLocation", data.selectedLocation());
        model.addAttribute("hasActivePostings", data.hasActivePostings());
        model.addAttribute("hasSelectedFilter", data.hasSelectedFilter());
        return "jobs/public-list";
    }

    /**
     * Existing protected job creation route for ADMIN and HR_MANAGER users.
     */
    @GetMapping("/new")
    public String createJob() {
        return "jobs/form";
    }
}
