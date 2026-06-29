package com.example.groupproject.entity;

import com.example.groupproject.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity ánh xạ bảng applications trong talenthub_schema.sql.
 *
 * Nguồn: G5-TalentHub, đổi package → com.example.groupproject
 *
 * Đồng bộ schema:
 *   id,
 *   job_id(FK→job_postings NOT NULL),
 *   candidate_id(FK→users NOT NULL),
 *   cover_letter(TEXT NULL),
 *   cv_filename(VARCHAR255 NOT NULL),
 *   cv_storage_path(VARCHAR500 NOT NULL),
 *   status(VARCHAR20 default APPLIED),
 *   status_changed_at(TIMESTAMP6 NOT NULL default NOW),
 *   submitted_at(TIMESTAMP6 NOT NULL default NOW),
 *   updated_at(TIMESTAMP6 NOT NULL ON UPDATE)
 *
 * Unique constraint: (job_id, candidate_id) — một ứng viên chỉ apply một job một lần.
 */
@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_application",
        columnNames = {"job_id", "candidate_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** FK → job_postings.id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    /** FK → users.id (ứng viên, role = CANDIDATE) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "cv_filename", nullable = false)
    private String cvFilename;

    @Column(name = "cv_storage_path", nullable = false, length = 500)
    private String cvStoragePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.submittedAt = now;
        this.statusChangedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
