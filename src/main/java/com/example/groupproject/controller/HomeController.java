package com.example.groupproject.controller;

import com.example.groupproject.entity.User;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/")
    public String home(HttpSession session) {
        User user = authService.getCurrentUser(session);
        if (user != null) {
            return "redirect:/profile";
        }
        return "user/index";
    }
}
