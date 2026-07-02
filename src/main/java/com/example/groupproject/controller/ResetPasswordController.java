package com.example.groupproject.controller;
import com.example.groupproject.dto.ForgotPasswordDTO;
import com.example.groupproject.dto.ResetPasswordDTO;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.EmailService;
import com.example.groupproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class ResetPasswordController {
    @Autowired
    private AuthService authService;
    @Autowired
    private EmailService emailService;
    @GetMapping("/forgot-password")
    public String showForgotPassword(Model model){
        if(!model.containsAttribute("forgotPasswordDTO")){
            model.addAttribute(
                    "forgotPasswordDTO",
                    new ForgotPasswordDTO()
            );
        }
        return "forgot-password";
    }
    @PostMapping("/forgot-password")
    public String forgotPassword(
            @Valid @ModelAttribute("forgotPasswordDTO") ForgotPasswordDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes ra){
        if(result.hasErrors()){
            return "forgot-password";
        }
        try {
            String otp = authService.createResetPasswordOtp(dto);
            emailService.sendResetPasswordEmail(dto.getEmail(), otp);
            ra.addFlashAttribute("msg", "OTP has been sent to your email");
            return "redirect:/reset-password";
        }catch(Exception e){
            model.addAttribute("error", e.getMessage());
            return "forgot-password";
        }
    }
    @GetMapping("/reset-password")
    public String showResetPassword(Model model){
        if(!model.containsAttribute("resetPasswordDTO")){
            model.addAttribute("resetPasswordDTO", new ResetPasswordDTO());
        }
        return "reset-password";
    }
    @PostMapping("/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordDTO") ResetPasswordDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("resetPasswordDTO", dto);
            return "reset-password";
        }

        try {
            authService.resetPassword(dto);
            ra.addFlashAttribute("msg", "Password reset successfully. Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("resetPasswordDTO", dto);
            return "reset-password";
        }
    }
}