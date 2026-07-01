package com.example.groupproject.controller;

import com.example.groupproject.dto.CreateUserForm;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.service.AuthService;
import com.example.groupproject.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý user — chỉ ADMIN truy cập (/admin/**).
 */
@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final AuthService authService;

    public UserManagementController(UserManagementService userManagementService, AuthService authService) {
        this.userManagementService = userManagementService;
        this.authService = authService;
    }

    @GetMapping
    public String listUsers(@RequestParam(required = false) String search,
                            @RequestParam(required = false) UserRole role,
                            @RequestParam(required = false) UserStatus status,
                            Model model,
                            HttpSession session) {
        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
        List<User> users = userManagementService.searchUsers(search, role, status);
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("createUserForm", new CreateUserForm());
        model.addAttribute("creatableRoles", List.of(UserRole.HR_MANAGER, UserRole.INTERVIEWER));
        addActionFlags(model, users);
        return "admin/users";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute("createUserForm") CreateUserForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
        if (bindingResult.hasErrors()) {
            populateListModel(model, null, null, null);
            return "admin/users";
        }
        try {
            User actor = authService.getCurrentUser(session);
            userManagementService.createUser(form, actor);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully");
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("createUser", ex.getMessage());
            populateListModel(model, null, null, null);
            return "admin/users";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Integer id,
                         RedirectAttributes redirectAttributes,
                         HttpSession session) {
        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
        try {
            userManagementService.unlockUser(id, authService.getCurrentUser(session));
            redirectAttributes.addFlashAttribute("successMessage", "Account unlocked");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Integer id,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        authService.requireRole(authService.getCurrentUser(session), UserRole.ADMIN);
        try {
            userManagementService.deactivateUser(id, authService.getCurrentUser(session));
            redirectAttributes.addFlashAttribute("successMessage", "Account deactivated");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void populateListModel(Model model, String search, UserRole role, UserStatus status) {
        List<User> users = userManagementService.searchUsers(search, role, status);
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("creatableRoles", List.of(UserRole.HR_MANAGER, UserRole.INTERVIEWER));
        addActionFlags(model, users);
    }

    private void addActionFlags(Model model, List<User> users) {
        Map<Integer, Boolean> canUnlock = new HashMap<>();
        Map<Integer, Boolean> canDeactivate = new HashMap<>();
        for (User user : users) {
            canUnlock.put(user.getId(), userManagementService.canUnlock(user));
            canDeactivate.put(user.getId(), userManagementService.canDeactivate(user));
        }
        model.addAttribute("canUnlock", canUnlock);
        model.addAttribute("canDeactivate", canDeactivate);
    }
}
