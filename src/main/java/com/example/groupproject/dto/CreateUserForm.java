package com.example.groupproject.dto;

import com.example.groupproject.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Form DTO để tạo user mới (HR_MANAGER hoặc INTERVIEWER).
 * Dùng bởi UserManagementController và UserManagementService.
 *
 * Validation đồng bộ với CHECK constraint trong schema:
 *   - username: REGEXP '^[A-Za-z0-9_]{4,50}$' (chk_username_format)
 *   - role: chỉ có thể là HR_MANAGER hoặc INTERVIEWER (CREATABLE_ROLES trong service)
 */
@Getter
@Setter
public class CreateUserForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_]{4,50}$",
             message = "Username must be 4-50 characters (letters, digits, underscores)")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "Password must contain at least one uppercase letter and one number")
    private String password;

    @NotBlank(message = "Role is required")
    private String role;

    /** Chuyển đổi role string → UserRole enum. Ném IllegalArgumentException nếu giá trị không hợp lệ. */
    public UserRole getUserRole() {
        return UserRole.valueOf(role);
    }
}
