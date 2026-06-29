package com.example.groupproject.entity;

import com.example.groupproject.entity.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entity ánh xạ bảng interviews trong talenthub_schema.sql.
 *
 * FIX: G5-TalentHub Interview entity bị THIẾU 6 field so với schema:
 *   - status (VARCHAR20, default SCHEDULED)   → thêm với enum InterviewStatus
 *   - rating (SMALLINT NULL, 1-5)             → thêm
 *   - feedback (TEXT NULL)                    → thêm
 *   - evaluated_at (TIMESTAMP6 NULL)          → thêm
 *   - assigned_by (FK→users NOT NULL)         → thêm
 *   - updated_at (TIMESTAMP6 NOT NULL)        → thêm
 *
 * Đồng bộ đầy đủ với schema:
 *   id,
 *   application_id(FK→applications NOT NULL),
 *   interviewer_id(FK→users NOT NULL),
 *   interview_date(DATE NOT NULL),
 *   interview_time(TIME NOT NULL),
 *   location_or_link(VARCHAR500 NULL),
 *   status(VARCHAR20 default SCHEDULED),
 *   rating(SMALLINT NULL, 1–5),
 *   feedback(TEXT NULL),
 *   evaluated_at(TIMESTAMP6 NULL),
 *   assigned_by(FK→users NOT NULL),
 *   created_at(TIMESTAMP6 NOT NULL),
 *   updated_at(TIMESTAMP6 NOT NULL)
 *
 * Business rule từ schema (chk_evaluated_fields):
 *   Nếu status=EVALUATED thì rating, feedback, evaluated_at đều phải NOT NULL.
 *   Được enforce ở application layer (service), không cần @Check annotation.
 */
@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** FK → applications.id (ON DELETE CASCADE theo schema) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    /** FK → users.id (interviewer, role = INTERVIEWER) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    /** DATE — ngày phỏng vấn */
    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    /** TIME — giờ phỏng vấn */
    @Column(name = "interview_time", nullable = false)
    private LocalTime interviewTime;

    /** Địa điểm hoặc link online (nullable) */
    @Column(name = "location_or_link", length = 500)
    private String locationOrLink;

    /**
     * Trạng thái phỏng vấn.
     * SCHEDULED (mặc định) → EVALUATED (sau khi interviewer nộp đánh giá).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    /**
     * Điểm đánh giá (1–5, nullable khi còn SCHEDULED).
     * SMALLINT trong schema → Short trong Java.
     */
    @Column(nullable = true)
    private Short rating;

    /**
     * Nhận xét của interviewer (nullable khi còn SCHEDULED).
     */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /**
     * Thời điểm nộp đánh giá (nullable khi còn SCHEDULED).
     */
    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    /**
     * FK → users.id (người assign interview này, NOT NULL).
     * Thường là HR_MANAGER hoặc ADMIN.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

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
