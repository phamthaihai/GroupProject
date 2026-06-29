package com.example.groupproject.entity;

import com.example.groupproject.entity.enums.ActivityEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity ánh xạ bảng activity_log trong talenthub_schema.sql.
 *
 * Nguồn: G5-TalentHub, đổi package → com.example.groupproject
 *
 * Đồng bộ schema:
 *   id(BIGINT AUTO_INCREMENT),
 *   actor_id(FK→users NULL — ON DELETE SET NULL),
 *   actor_username(VARCHAR50 NOT NULL),
 *   event_type(VARCHAR50 NOT NULL),
 *   description(TEXT NULL),
 *   ip_address(VARCHAR45 NULL),
 *   created_at(TIMESTAMP6 NOT NULL)
 *
 * Note: actor_id nullable để hỗ trợ ON DELETE SET NULL khi user bị xóa.
 */
@Entity
@Table(name = "activity_log")
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog {

    /** BIGINT trong schema */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK → users.id, nullable (ON DELETE SET NULL).
     * Nếu actor bị xóa, actor_id = NULL nhưng actor_username vẫn giữ lại.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Tên đăng nhập của actor tại thời điểm log (không thay đổi kể cả khi user bị xóa) */
    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ActivityEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** IPv4 (15 chars) hoặc IPv6 (45 chars) */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
