package com.example.groupproject.controller;

import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller xử lý đăng nhập, đăng xuất và redirect trang chủ.
 */
@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (authService.isAuthenticated(session)) {
            return "redirect:/profile";
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session) {
        if (authService.login(username, password, session)) {
            return "redirect:/profile";
        }
        return "redirect:/login?error";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/login?logout";
    }

    /** Root URL redirect đến profile */
    @GetMapping("/")
    public String home() {
        return "redirect:/profile";
    }
}
