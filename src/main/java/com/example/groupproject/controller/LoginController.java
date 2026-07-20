package com.example.groupproject.controller;

import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.exception.AccountLockedException;
import com.example.groupproject.exception.AdminLockedException;
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
import org.springframework.web.bind.annotation.RequestParam;
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
            @RequestParam(required = false) String redirect,
            @ModelAttribute("msg") String msg,
            @ModelAttribute("err") String err,
            Model model,
            HttpSession session
    ) {
        if (redirect != null && !redirect.isBlank()) {
            session.setAttribute("redirectUrl", redirect);
        }
        User currentUser = authService.getCurrentUser(session);

        if (currentUser != null) {
            if (currentUser.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            if (currentUser.getRole() == UserRole.CANDIDATE) {
                return "redirect:/candidate/applications";
            }
            if (currentUser.getRole() == UserRole.HR_MANAGER) {
                return "redirect:/hr/dashboard";
            }
            if (currentUser.getRole() == UserRole.INTERVIEWER) {
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

        try {
            User user = authService.login(loginDTO, session);

            ra.addFlashAttribute("msg", "Đăng nhập thành công");

            String redirectUrl = (String) session.getAttribute("redirectUrl");
            if (redirectUrl != null && !redirectUrl.isBlank()) {
                session.removeAttribute("redirectUrl");
                return "redirect:" + redirectUrl;
            }

            if (user.getRole() != null) {
                switch (user.getRole()) {
                    case ADMIN:
                        return "redirect:/admin/dashboard"; // SCR-07
                    case HR_MANAGER:
                        return "redirect:/hr/dashboard";    // SCR-06
                    case INTERVIEWER:
                        return "redirect:/interviewer/applications"; // SCR-17 (Sửa lại route của bạn nếu cần)
                    case CANDIDATE:
                        return "redirect:/candidate/applications";   // SCR-15
                    default:
                        break;
                }
            }
            return "redirect:/profile";

        } catch (AdminLockedException e) {
            // HƯỚNG 1: Admin khóa -> Sang trang HTML thông báo riêng biệt
            return "auth/admin-locked-page";

        } catch (AccountLockedException e) {
            // HƯỚNG 2a: Sai quá 5 lần -> Về lại trang login kèm param hiển thị Lockout Banner
            return "redirect:/login?error=locked";

        } catch (IllegalArgumentException e) {
            // HƯỚNG 2b: Sai pass/email thông thường -> Về lại trang login kèm param hiển thị Generic Banner
            return "redirect:/login?error=generic";

        } catch (Exception e) {
            // Dự phòng các lỗi hệ thống không lường trước khác
            return "redirect:/login?error=generic";
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