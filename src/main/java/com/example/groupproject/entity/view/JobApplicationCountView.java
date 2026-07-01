package com.example.groupproject.entity.view;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_job_application_counts")
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

    public Integer getJobId() {
        return jobId;
    }

    public Long getTotal() {
        return total;
    }

    public Long getApplied() {
        return applied;
    }

    public Long getScreening() {
        return screening;
    }

    public Long getInterview() {
        return interview;
    }

    public Long getOffer() {
        return offer;
    }

    public Long getHired() {
        return hired;
    }

    public Long getRejected() {
        return rejected;
    }

    public Long getWithdrawn() {
        return withdrawn;
    }
}
