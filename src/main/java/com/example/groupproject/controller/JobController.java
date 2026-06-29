package com.example.groupproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho Job Posting management.
 * Yêu cầu role ADMIN hoặc HR_MANAGER (bảo vệ bởi SecurityConfig: /jobs/**).
 */
@Controller
@RequestMapping("/jobs")
public class JobController {

    /** GET /jobs/new — hiển thị form tạo job mới */
    @GetMapping("/new")
    public String createJob() {
        return "jobs/form";
    }
}
