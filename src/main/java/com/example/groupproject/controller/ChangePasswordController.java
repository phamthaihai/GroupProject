package com.example.groupproject.controller;
import com.example.groupproject.dto.ChangePasswordDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller
public class ChangePasswordController {

    @Autowired
    private AuthService authService;

    @GetMapping("/change-password")
    public String showChangePassword(Model model, HttpSession session){
        if (authService.getCurrentUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
            BindingResult result,
            Model model,
            HttpSession session) {

        User user = (User) authService.getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            return "auth/change-password";
        }

        try {
            authService.changePassword(
                    user.getId(),
                    dto.getOldPassword(),
                    dto.getNewPassword(),
                    dto.getConfirmPassword()
            );
            session.invalidate();
            return "redirect:/login";

        } catch (Exception e) {
            String errorMsg = e.getMessage();

            if (errorMsg.contains("Incorrect current password")) {
                result.rejectValue("oldPassword", "error.oldPassword", errorMsg);
            }
            else if (errorMsg.contains("different from your current password")) {
                result.rejectValue("newPassword", "error.newPassword", errorMsg);
            }
            else if (errorMsg.contains("do not match")) {
                result.rejectValue("confirmPassword", "error.confirmPassword", errorMsg);
            }
            else {
                model.addAttribute("passwordError", errorMsg);
            }

            return "auth/change-password";
        }
    }
}