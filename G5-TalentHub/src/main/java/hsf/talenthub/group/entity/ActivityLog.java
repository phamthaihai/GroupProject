package hsf.talenthub.group.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.net.InetAddress;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "activity_log")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;

    @Column(name = "event_type", columnDefinition = "activity_event_type not null")
    private Object eventType;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}