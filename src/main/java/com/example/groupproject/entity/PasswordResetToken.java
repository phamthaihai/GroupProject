package com.example.groupproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity ánh xạ bảng password_reset_tokens trong talenthub_schema.sql.
 *
 * Giữ nguyên từ GroupProject, chỉ:
 * - Cập nhật import (User vẫn cùng package, không đổi)
 * - Chuyển ZonedDateTime → Instant (nhất quán toàn project)
 * - Thêm Lombok
 *
 * Đồng bộ schema:
 *   id, user_id(FK→users), token(UNIQUE), expires_at, used_at(NULL), created_at
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PasswordResetToken(User user, String token, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}