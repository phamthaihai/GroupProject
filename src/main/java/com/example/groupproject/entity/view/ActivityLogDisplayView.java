package com.example.groupproject.entity.view;

import com.example.groupproject.entity.enums.ActivityEventType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Immutable
@Table(name = "v_activity_log_display")
public class ActivityLogDisplayView {

    @Id
    private Long id;

    @Column(name = "actor_id")
    private Integer actorId;

    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false, length = 50)
    private ActivityEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "actor_display_name")
    private String actorDisplayName;

    public Long getId() {
        return id;
    }

    public Integer getActorId() {
        return actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public ActivityEventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }
}
