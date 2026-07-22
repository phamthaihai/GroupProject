package com.example.groupproject.controller;

import com.example.groupproject.dto.ApplicationDetailDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.service.ApplicationDetailService;
import com.example.groupproject.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ApplicationDetailController {

    private final ApplicationDetailService applicationDetailService;
    private final AuthService authService;

    public ApplicationDetailController(ApplicationDetailService applicationDetailService, AuthService authService) {
        this.applicationDetailService = applicationDetailService;
        this.authService = authService;
    }

    // SCR-17: Xem chi tiết ứng tuyển
    @GetMapping("/applications/{id}")
    public String getApplicationDetail(@PathVariable Long id,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes,
                                       HttpServletRequest request) {

        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Kiểm tra quyền cơ bản theo Role
        boolean hasPermission = authService.hasAnyRole(currentUser,
                UserRole.ADMIN,
                UserRole.HR_MANAGER,
                UserRole.INTERVIEWER);

        if (!hasPermission) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập.");
            return redirectBasedOnRole(currentUser);
        }

        try {
            ApplicationDetailDTO applicationDetail = applicationDetailService.getApplicationDetail(id, currentUser);

            model.addAttribute("appDetail", applicationDetail);
            model.addAttribute("currentUser", currentUser);

        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectBasedOnRole(currentUser);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải dữ liệu đơn ứng tuyển.");
            return redirectBasedOnRole(currentUser);
        }

        return "hr/detail";
    }

    // SCR-17: Xử lý chuyển trạng thái ứng tuyển (Move to Screening / Interview / Offer)
    @PostMapping("/applications/{id}/advance")
    public String advanceApplication(@PathVariable Long id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean hasPermission = authService.hasAnyRole(currentUser,
                UserRole.ADMIN,
                UserRole.HR_MANAGER);

        if (!hasPermission) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thao tác này.");
            return redirectBasedOnRole(currentUser);
        }

        try {
            applicationDetailService.advanceApplicationStatus(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn ứng tuyển thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật trạng thái: " + e.getMessage());
        }

        return "redirect:/applications/" + id;
    }

    // SCR-17: Thêm ghi chú nội bộ (Internal Note)
    @PostMapping("/applications/{id}/notes")
    public String addNote(@PathVariable Long id,
                          @RequestParam("content") String content, // Đã sửa: Khớp với name="content" trong HTML
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean hasPermission = authService.hasAnyRole(currentUser,
                UserRole.ADMIN,
                UserRole.HR_MANAGER);

        if (!hasPermission) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thêm ghi chú.");
            return redirectBasedOnRole(currentUser);
        }

        try {
            applicationDetailService.addNote(id, content, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm ghi chú thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm ghi chú: " + e.getMessage());
        }

        return "redirect:/applications/" + id;
    }

    // Hàm bắc cầu: Tìm đơn theo UserId
    @GetMapping("/applications/user/{userId}")
    public String getAppByUserId(@PathVariable Long userId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User currentUser = authService.getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Long appId = applicationDetailService.findAppIdByUserId(userId);

        if (appId != null) {
            return "redirect:/applications/" + appId;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Người dùng này chưa có đơn ứng tuyển.");
            return redirectBasedOnRole(currentUser);
        }
    }

    private String redirectBasedOnRole(User user) {
        if (user == null) return "redirect:/login";

        if (user.getRole() == UserRole.ADMIN) {
            return "redirect:/admin/dashboard";
        } else if (user.getRole() == UserRole.HR_MANAGER) {
            return "redirect:/hr/dashboard";
        } else if (user.getRole() == UserRole.INTERVIEWER) {
            return "redirect:/hr/dashboard";
        } else {
            return "redirect:/";
        }
    }
}