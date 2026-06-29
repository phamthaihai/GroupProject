package com.example.groupproject.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Read-only entity ánh xạ VIEW v_job_application_counts trong talenthub_schema.sql.
 *
 * VIEW definition (schema):
 *   SELECT job_id,
 *          COUNT(*) AS total,
 *          CAST(SUM(CASE WHEN status='APPLIED'   THEN 1 ELSE 0 END) AS SIGNED) AS applied,
 *          CAST(SUM(CASE WHEN status='SCREENING' THEN 1 ELSE 0 END) AS SIGNED) AS screening,
 *          CAST(SUM(CASE WHEN status='INTERVIEW' THEN 1 ELSE 0 END) AS SIGNED) AS interview,
 *          CAST(SUM(CASE WHEN status='OFFER'     THEN 1 ELSE 0 END) AS SIGNED) AS offer,
 *          CAST(SUM(CASE WHEN status='HIRED'     THEN 1 ELSE 0 END) AS SIGNED) AS hired,
 *          CAST(SUM(CASE WHEN status='REJECTED'  THEN 1 ELSE 0 END) AS SIGNED) AS rejected,
 *          CAST(SUM(CASE WHEN status='WITHDRAWN' THEN 1 ELSE 0 END) AS SIGNED) AS withdrawn
 *   FROM applications GROUP BY job_id
 *
 * Dùng cho DashboardService để hiển thị số lượng application theo từng trạng thái.
 */
@Entity
@Immutable
@Table(name = "v_job_application_counts")
@Getter
public class JobApplicationCountView {

    @Id
    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "total")
    private Long total;

    @Column(name = "applied")
    private Long applied;

    @Column(name = "screening")
    private Long screening;

    @Column(name = "interview")
    private Long interview;

    @Column(name = "offer")
    private Long offer;

    @Column(name = "hired")
    private Long hired;

    @Column(name = "rejected")
    private Long rejected;

    @Column(name = "withdrawn")
    private Long withdrawn;
}
