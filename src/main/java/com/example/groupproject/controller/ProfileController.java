package com.example.groupproject.controller;

import com.example.groupproject.entity.User;
import com.example.groupproject.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller hiển thị profile và đổi mật khẩu.
 * Tất cả authenticated user đều có thể truy cập.
 */
@Controller
@RequiredArgsConstructor
public class ProfileController {

    /** GET /profile — hiển thị thông tin tài khoản của user hiện tại */
    @GetMapping("/profile")
    public String profile(Model model) {
        User user = SecurityUtils.getCurrentUser();
        model.addAttribute("user", user);
        return "profile";
    }

    /** GET /change-password — hiển thị form đổi mật khẩu */
    @GetMapping("/change-password")
    public String changePassword() {
        return "change-password";
    }
}
