package com.example.groupproject.config;

import com.example.groupproject.entity.User;
import com.example.groupproject.security.SecurityUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice inject currentUser vào tất cả Model.
 * Template Thymeleaf có thể dùng ${currentUser} để hiển thị thông tin user.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUser")
    public User currentUser() {
        return SecurityUtils.getCurrentUser();
    }
}
