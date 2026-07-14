package com.example.groupproject.controller;

import com.example.groupproject.dto.ApplicationDetailDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.service.ApplicationDetailService;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ApplicationDetailController {

    private final ApplicationDetailService applicationDetailService;
    private final AuthService authService;

    public ApplicationDetailController(ApplicationDetailService applicationDetailService, AuthService authService) {
        this.applicationDetailService = applicationDetailService;
        this.authService = authService;
    }

    // Hàm 1: Xem chi tiết ứng tuyển
    @GetMapping("/applications/{id}")
    public String getApplicationDetail(@PathVariable Long id,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean hasPermission = authService.hasAnyRole(currentUser,
                com.example.groupproject.entity.enums.UserRole.ADMIN,
                com.example.groupproject.entity.enums.UserRole.HR_MANAGER,
                com.example.groupproject.entity.enums.UserRole.INTERVIEWER);

        if (!hasPermission) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập.");
            return "redirect:/";
        }

        try {
            ApplicationDetailDTO applicationDetail = applicationDetailService.getApplicationDetail(id, currentUser);
            model.addAttribute("appDetail", applicationDetail);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi tải dữ liệu.");
            return "redirect:/applications";
        }

        return "hr/detail";
    }

    // Hàm 2: Hàm bắc cầu (Nằm bên ngoài hàm trên, bên trong Controller)
    @GetMapping("/applications/user/{userId}")
    public String getAppByUserId(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        Long appId = applicationDetailService.findAppIdByUserId(userId);

        if (appId != null) {
            return "redirect:/applications/" + appId;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng này chưa có đơn ứng tuyển.");
            return "redirect:/admin/users";
        }
    }
}