package com.example.groupproject.entity;

import com.example.groupproject.entity.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity ánh xạ bảng job_postings trong talenthub_schema.sql.
 *
 * Nguồn: G5-TalentHub, đổi package → com.example.groupproject
 *
 * Đồng bộ schema:
 *   id, title(200), department(100), location(100), description(TEXT),
 *   requirements(TEXT NULL), salary_range(100 NULL),
 *   application_deadline(DATE NULL), status(VARCHAR20 default DRAFT),
 *   created_by(FK→users NOT NULL),
 *   created_at(TIMESTAMP6), updated_at(TIMESTAMP6)
 */
@Entity
@Table(name = "job_postings")
@Getter
@Setter
@NoArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    /** DATE (LocalDate) — deadline nullable theo schema */
    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    /** FK → users.id (người tạo job, NOT NULL) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
