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
import com.example.groupproject.service.UserService;

@Controller
public class LoginController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String showFormLogin(@ModelAttribute("msg") String msg,
                                @ModelAttribute("err") String err,
                                Model model,
                                HttpSession session) {

        User currentUser = (User) session.getAttribute("user");

        if (currentUser != null) {

            if (currentUser.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
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

        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@Valid @ModelAttribute("loginDTO") LoginDTO loginDTO,
                            BindingResult result,
                            Model model,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "login";
        }

        User user = authService.login(loginDTO);

        if (user == null) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng hoặc tài khoản chưa được kích hoạt");
            return "login";
        }

        // Lưu user vào session
        session.setAttribute("user", user);
        ra.addFlashAttribute("msg", "Đăng nhập thành công");

        // Điều hướng theo role sau khi đăng nhập
        if (user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().name())) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("msg", "Bạn đã đăng xuất");
        return "redirect:/";
    }
}