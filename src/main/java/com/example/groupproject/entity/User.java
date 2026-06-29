package com.example.groupproject.entity;

import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity ánh xạ bảng users trong talenthub_schema.sql.
 *
 * Merged từ:
 *   - GroupProject/entity/User.java  → giữ @PrePersist/@PreUpdate, constructor full
 *   - G5-TalentHub/model/entity/User.java → Lombok, Instant, enum tách file
 *
 * Đồng bộ schema:
 *   id, full_name, username(50), email, password_hash,
 *   role(VARCHAR20), status(VARCHAR20, default ACTIVE),
 *   failed_login_count(SMALLINT, default 0), locked_at(TIMESTAMP6 NULL),
 *   created_at(TIMESTAMP6 NOT NULL), updated_at(TIMESTAMP6 NOT NULL)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * SMALLINT trong schema → short/Short trong Java.
     * Giữ kiểu Short (wrapper) để tương thích nullable context.
     */
    @Column(name = "failed_login_count", nullable = false)
    private Short failedLoginCount = 0;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Constructor đầy đủ (giữ từ GroupProject, hữu ích cho testing và DataInitializer).
     */
    public User(Integer id, String fullName, String username, String email,
                String passwordHash, UserRole role, UserStatus status,
                Short failedLoginCount, Instant lockedAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.failedLoginCount = failedLoginCount != null ? failedLoginCount : 0;
        this.lockedAt = lockedAt;
    }

    /**
     * Tự động set created_at và updated_at khi persist lần đầu.
     * Giữ từ GroupProject — tốt hơn cách G5 dùng Instant.now() inline
     * vì đảm bảo giá trị luôn được set ngay trước khi INSERT.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Tự động update updated_at khi entity được UPDATE.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}