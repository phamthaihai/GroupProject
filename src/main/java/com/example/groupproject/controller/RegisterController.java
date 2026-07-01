package com.example.groupproject.controller;
import com.example.groupproject.dto.RegisterDTO;
import com.example.groupproject.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@Controller
public class RegisterController {
    private final AuthService authService;
    public RegisterController(AuthService authService){
        this.authService = authService;
    }
    @GetMapping("/register")
    public String registerPage(Model model){
        if(!model.containsAttribute("registerDTO")){
            model.addAttribute("registerDTO", new RegisterDTO());
        }
        return "register";
    }
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerDTO") RegisterDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes ra){
        if(result.hasErrors()){
            return "register";
        }
        try {
            String baseUrl = ServletUriComponentsBuilder
                            .fromCurrentContextPath()
                            .build()
                            .toUriString();
            authService.register(dto, baseUrl);
            ra.addFlashAttribute("msg", "Register success. Check your email.");
            return "redirect:/login";
        }catch(Exception e){
            model.addAttribute("err", e.getMessage()
            );
            return "register";
        }
    }
    @GetMapping("/verify")
    public String verify(
            @RequestParam String token,
            RedirectAttributes ra){
        try{
            authService.verifyEmail(token);
            ra.addFlashAttribute("msg", "Email verified successfully");
        }catch(Exception e){
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/login";
    }
}