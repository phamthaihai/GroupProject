package com.example.groupproject.controller;

import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.groupproject.dto.LoginDTO;
import com.example.groupproject.entity.User;

@Controller
public class LoginController {
    public static final String SESSION_USER_ID = "loggedInUserId";

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showFormLogin(
            @ModelAttribute("msg") String msg,
            @ModelAttribute("err") String err,
            Model model,
            HttpSession session
    ) {
        //Lấy user
        User currentUser = authService.getCurrentUser(session);

        if (currentUser != null) {
            if (currentUser.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            if (currentUser.getRole() == UserRole.CANDIDATE) {
                return "redirect:/candidate/applications";
            }
            return "redirect:/";
        }


        if (!model.containsAttribute("loginDTO")) {
            model.addAttribute("loginDTO", new LoginDTO());
        }

        if (msg != null && !msg.isBlank()) {
            model.addAttribute("success", msg);
        }

        if (err != null && !err.isBlank()) {
            model.addAttribute("error", err);
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String loginUser(
            @Valid @ModelAttribute("loginDTO") LoginDTO loginDTO,
            BindingResult result,
            Model model,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (result.hasErrors()) {
            return "auth/login";
        }

        try{
            //kiểm tra thông tin đăng nhập
            User user = authService.login(loginDTO,session);

            // Thông báo đăng nhập thành công
            ra.addFlashAttribute("msg", "Đăng nhập thành công");

            // Điều hướng theo role sau khi đăng nhập
            if (user.getRole() != null && user.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            if (user.getRole() != null && user.getRole() == UserRole.CANDIDATE) {
                return "redirect:/candidate/applications";
            }

            return "redirect:/profile";
        }catch (Exception errorMessage){
            model.addAttribute("error", errorMessage.getMessage());
            return "auth/login";
        }
    }

    @PostMapping("/logout")
    public String logout(
            HttpSession session,
            RedirectAttributes ra
    ) {
        ra.addFlashAttribute("msg", "Bạn đã đăng xuất");
        authService.doLogout(session);
        return "redirect:/login";
    }
}