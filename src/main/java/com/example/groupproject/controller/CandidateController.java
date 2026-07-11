package com.example.groupproject.controller;

import com.example.groupproject.entity.Application;
import com.example.groupproject.entity.User;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.ApplicationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/candidate/applications")
public class CandidateController {

    private final AuthService authService;
    private final ApplicationService applicationService;

    public CandidateController(AuthService authService, ApplicationService applicationService) {
        this.authService = authService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public String listApplications(@RequestParam(required = false, defaultValue = "ALL") String status,
                                   HttpSession session, Model model) {
        User currentUser = authService.getCurrentUser(session);
        List<Application> apps = applicationService.getApplicationsByCandidate(currentUser.getId());

        List<Application> filteredApps = apps;
        if (!"ALL".equals(status)) {
            filteredApps = apps.stream()
                    .filter(a -> a.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        model.addAttribute("applications", filteredApps);
        model.addAttribute("currentStatus", status.toUpperCase());
        model.addAttribute("hasApplications", !apps.isEmpty());

        return "candidate/applications";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        User currentUser = authService.getCurrentUser(session);
        try {
            applicationService.withdrawApplication(id, currentUser);
            ra.addFlashAttribute("successMessage", "Your application has been withdrawn successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/applications";
    }
}
