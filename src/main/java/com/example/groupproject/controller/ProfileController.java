package com.example.groupproject.controller;

import com.example.groupproject.entity.User;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller hiển thị profile và đổi mật khẩu.
 */
@Controller
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        User user = authService.getCurrentUser(session);
        authService.requireAuthenticated(user);
        model.addAttribute("user", user);
        return "profile";
    }
//
//    @GetMapping("/change-password")
//    public String changePassword(HttpSession session) {
//        authService.requireAuthenticated(authService.getCurrentUser(session));
//        return "change-password";
//    }
}
