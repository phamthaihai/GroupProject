package com.example.groupproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller xử lý đăng nhập và redirect trang chủ.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Root URL redirect đến profile */
    @GetMapping("/")
    public String home() {
        return "redirect:/profile";
    }
}
