package com.example.groupproject.repository;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho entity User.
 * Cung cấp các query cần thiết cho:
 *   - Authentication (findByUsername)
 *   - Dashboard stats (countBy*)
 *   - Admin user management (searchUsers)
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    /** Đếm số user theo role — dùng cho admin dashboard */
    long countByRole(UserRole role);

    /** Đếm số user theo status — dùng cho admin dashboard (lockedCount) */
    long countByStatus(UserStatus status);

    /** Đếm user theo cả role và status — dùng để kiểm tra "last active admin" */
    long countByRoleAndStatus(UserRole role, UserStatus status);

    /**
     * Tìm kiếm user với filter linh hoạt.
     * Dùng cho admin/users page.
     *
     * @param search tìm kiếm theo fullName hoặc email (case-insensitive), null = bỏ qua
     * @param role   lọc theo role, null = bỏ qua
     * @param status lọc theo status, null = bỏ qua
     */
    @Query("SELECT u FROM User u WHERE " +
            "(cast(:role as string) IS NULL OR u.role = :role) AND " +
            "(cast(:status as string) IS NULL OR u.status = :status) " +
            "ORDER BY u.createdAt DESC")
    List<User> findByRoleAndStatusFiltered(@Param("role") UserRole role,
                                           @Param("status") UserStatus status);


        Optional<User> findByEmail(String email);
        User findByUsername(String username);
        boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    Optional<User> findByVerifyToken(String verifyToken);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(String email, String fullName);
    /** Tìm tất cả user là INTERVIEWER và đang ACTIVE để làm dropdown */
    List<User> findByRoleAndStatusOrderByFullNameAsc(UserRole role, UserStatus status);
}
