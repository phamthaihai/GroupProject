package com.example.groupproject.entity.view;

import com.example.groupproject.entity.enums.ActivityEventType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Read-only entity ánh xạ VIEW v_activity_log_display trong talenthub_schema.sql.
 *
 * VIEW definition (schema):
 *   SELECT al.id, al.actor_id, al.actor_username, al.event_type,
 *          al.description, al.ip_address, al.created_at,
 *          CASE WHEN u.status='INACTIVE' THEN CONCAT(al.actor_username,' (deactivated)')
 *               ELSE al.actor_username END AS actor_display_name
 *   FROM activity_log al LEFT JOIN users u ON u.id = al.actor_id
 *
 * @Immutable: Hibernate sẽ không tạo UPDATE/INSERT cho entity này.
 * Dùng cho màn hình hiển thị activity log (admin/activity-log.html).
 */
@Entity
@Immutable
@Table(name = "v_activity_log_display")
@Getter
public class ActivityLogDisplayView {

    @Id
    private Long id;

    @Column(name = "actor_id")
    private Integer actorId;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ActivityEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Computed column từ VIEW: tên hiển thị có ghi chú (deactivated) nếu cần */
    @Column(name = "actor_display_name")
    private String actorDisplayName;
}
