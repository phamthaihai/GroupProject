package com.example.groupproject.controller;
import com.example.groupproject.dto.ChangePasswordDTO;
import com.example.groupproject.entity.User;
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
    private UserService userService;
    @GetMapping("/change-password")
    public String showChangePassword(Model model){
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        return "change-password";
    }
    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
            BindingResult result,
            Model model,
            HttpSession session){
        User user =
                (User) session.getAttribute("user");
        if(user == null){
            return "redirect:/login";
        }
        if(result.hasErrors()){
            model.addAttribute("currentUser", user);
            model.addAttribute("activeTab", "password"
            );
            return "profile";
        }
        try {
            userService.changePassword(
                    user.getId(),
                    dto.getOldPassword(),
                    dto.getNewPassword(),
                    dto.getConfirmPassword()
            );
            session.invalidate();
            return "redirect:/login";
        }catch(Exception e){
            model.addAttribute("currentUser", user);
            model.addAttribute("passwordError", e.getMessage());
            model.addAttribute("activeTab", "password");
            return "profile";
        }
    }
}