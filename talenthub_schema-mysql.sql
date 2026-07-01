-- ============================================================
-- TalentHub Recruitment Management System
-- Database Schema (MySQL 8+)
-- Derived strictly from Group_Project.pdf
-- ============================================================

CREATE DATABASE IF NOT EXISTS talenthub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE talenthub;

-- ============================================================
-- 1. USERS
-- ============================================================

CREATE TABLE users (
    id                  INT             AUTO_INCREMENT PRIMARY KEY,

    full_name           VARCHAR(255)    NOT NULL,
    username            VARCHAR(50)     NOT NULL UNIQUE,
    email               VARCHAR(255)    NOT NULL UNIQUE,

    password_hash       VARCHAR(255)    NOT NULL,

    role                VARCHAR(20)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',

    failed_login_count  SMALLINT        NOT NULL DEFAULT 0,
    locked_at           TIMESTAMP(6)    NULL,

    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'HR_MANAGER', 'INTERVIEWER', 'CANDIDATE')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'INACTIVE')),
    CONSTRAINT chk_username_format CHECK (username REGEXP '^[A-Za-z0-9_]{4,50}$')
);

-- ============================================================
-- 2. PASSWORD_RESET_TOKENS
-- ============================================================

CREATE TABLE password_reset_tokens (
    id          INT             AUTO_INCREMENT PRIMARY KEY,
    user_id     INT             NOT NULL,
    token       VARCHAR(255)    NOT NULL UNIQUE,
    expires_at  TIMESTAMP(6)    NOT NULL,
    used_at     TIMESTAMP(6)    NULL,
    created_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- 3. JOB_POSTINGS
-- ============================================================

CREATE TABLE job_postings (
    id                  INT             AUTO_INCREMENT PRIMARY KEY,

    title               VARCHAR(200)    NOT NULL,
    department          VARCHAR(100)    NOT NULL,
    location            VARCHAR(100)    NOT NULL,
    description         TEXT            NOT NULL,

    requirements        TEXT            NULL,
    salary_range        VARCHAR(100)    NULL,
    application_deadline DATE           NULL,

    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',

    created_by          INT             NOT NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_jobs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_jobs_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
    -- deadline >= today enforced at application layer (MySQL CHECK cannot use CURDATE())
);

-- ============================================================
-- 4. APPLICATIONS
-- ============================================================

CREATE TABLE applications (
    id              INT                 AUTO_INCREMENT PRIMARY KEY,

    job_id          INT                 NOT NULL,
    candidate_id    INT                 NOT NULL,

    cover_letter    TEXT                NULL,
    cv_filename     VARCHAR(255)        NOT NULL,
    cv_storage_path VARCHAR(500)        NOT NULL,

    status          VARCHAR(20)         NOT NULL DEFAULT 'APPLIED',

    status_changed_at TIMESTAMP(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    submitted_at    TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_apps_job FOREIGN KEY (job_id) REFERENCES job_postings(id),
    CONSTRAINT fk_apps_candidate FOREIGN KEY (candidate_id) REFERENCES users(id),
    CONSTRAINT uq_application UNIQUE (job_id, candidate_id),
    CONSTRAINT chk_apps_status CHECK (status IN (
        'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED', 'WITHDRAWN'
    ))
);

-- ============================================================
-- 5. APPLICATION_NOTES
-- ============================================================

CREATE TABLE application_notes (
    id              INT             AUTO_INCREMENT PRIMARY KEY,
    application_id  INT             NOT NULL,
    author_id       INT             NOT NULL,
    content         TEXT            NOT NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_notes_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- ============================================================
-- 6. INTERVIEWS
-- ============================================================

CREATE TABLE interviews (
    id                  INT                 AUTO_INCREMENT PRIMARY KEY,
    application_id      INT                 NOT NULL,

    interviewer_id      INT                 NOT NULL,

    interview_date      DATE                NOT NULL,
    interview_time      TIME                NOT NULL,

    location_or_link    VARCHAR(500)        NULL,

    status              VARCHAR(20)         NOT NULL DEFAULT 'SCHEDULED',

    rating              SMALLINT            NULL,
    feedback            TEXT                NULL,
    evaluated_at        TIMESTAMP(6)        NULL,

    assigned_by         INT                 NOT NULL,
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_interviews_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_interviews_interviewer FOREIGN KEY (interviewer_id) REFERENCES users(id),
    CONSTRAINT fk_interviews_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT chk_interviews_status CHECK (status IN ('SCHEDULED', 'EVALUATED')),
    CONSTRAINT chk_rating_range CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5)),
    CONSTRAINT chk_evaluated_fields CHECK (
        status = 'SCHEDULED'
        OR (status = 'EVALUATED' AND rating IS NOT NULL AND feedback IS NOT NULL AND evaluated_at IS NOT NULL)
    )
    -- interview_date >= today enforced at application layer (MySQL CHECK cannot use CURDATE())
);

-- ============================================================
-- 7. ACTIVITY_LOG
-- ============================================================

CREATE TABLE activity_log (
    id              BIGINT              AUTO_INCREMENT PRIMARY KEY,

    actor_id        INT                 NULL,
    actor_username  VARCHAR(50)         NOT NULL,

    event_type      VARCHAR(50)         NOT NULL,

    description     TEXT                NULL,

    ip_address      VARCHAR(45)         NULL,

    created_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_log_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_log_event_type CHECK (event_type IN (
        'SIGN_IN_SUCCESS', 'SIGN_IN_FAILURE', 'ACCOUNT_CREATED', 'ACCOUNT_DEACTIVATED',
        'ACCOUNT_UNLOCKED', 'ACCOUNT_LOCKED', 'APPLICATION_STATUS_CHANGED',
        'CV_DOWNLOADED', 'EVALUATION_SUBMITTED'
    ))
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_role           ON users(role);
CREATE INDEX idx_users_status         ON users(status);
CREATE INDEX idx_users_email          ON users(email);

CREATE INDEX idx_jobs_status          ON job_postings(status);
CREATE INDEX idx_jobs_created_by      ON job_postings(created_by);
CREATE INDEX idx_jobs_department      ON job_postings(department);

CREATE INDEX idx_apps_job_id          ON applications(job_id);
CREATE INDEX idx_apps_candidate_id    ON applications(candidate_id);
CREATE INDEX idx_apps_status          ON applications(status);
CREATE INDEX idx_apps_submitted_at    ON applications(submitted_at);

CREATE INDEX idx_notes_application_id ON application_notes(application_id);

CREATE INDEX idx_interviews_app_id       ON interviews(application_id);
CREATE INDEX idx_interviews_interviewer  ON interviews(interviewer_id);
CREATE INDEX idx_interviews_status       ON interviews(status);
CREATE INDEX idx_interviews_date         ON interviews(interview_date);

CREATE INDEX idx_log_event_type    ON activity_log(event_type);
CREATE INDEX idx_log_actor_id      ON activity_log(actor_id);
CREATE INDEX idx_log_created_at    ON activity_log(created_at DESC);

CREATE INDEX idx_prt_user_id       ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_token         ON password_reset_tokens(token);

-- ============================================================
-- VIEWS
-- ============================================================

CREATE VIEW v_job_application_counts AS
SELECT
    job_id,
    COUNT(*)                                                                        AS total,
    CAST(SUM(CASE WHEN status = 'APPLIED'   THEN 1 ELSE 0 END) AS SIGNED)         AS applied,
    CAST(SUM(CASE WHEN status = 'SCREENING' THEN 1 ELSE 0 END) AS SIGNED)         AS screening,
    CAST(SUM(CASE WHEN status = 'INTERVIEW' THEN 1 ELSE 0 END) AS SIGNED)         AS interview,
    CAST(SUM(CASE WHEN status = 'OFFER'     THEN 1 ELSE 0 END) AS SIGNED)         AS offer,
    CAST(SUM(CASE WHEN status = 'HIRED'     THEN 1 ELSE 0 END) AS SIGNED)         AS hired,
    CAST(SUM(CASE WHEN status = 'REJECTED'  THEN 1 ELSE 0 END) AS SIGNED)         AS rejected,
    CAST(SUM(CASE WHEN status = 'WITHDRAWN' THEN 1 ELSE 0 END) AS SIGNED)         AS withdrawn
FROM applications
GROUP BY job_id;

CREATE VIEW v_activity_log_display AS
SELECT
    al.id,
    al.actor_id,
    al.actor_username,
    al.event_type,
    al.description,
    al.ip_address,
    al.created_at,
    CASE
        WHEN u.status = 'INACTIVE' THEN CONCAT(al.actor_username, ' (deactivated)')
        ELSE al.actor_username
    END AS actor_display_name
FROM activity_log al
LEFT JOIN users u ON u.id = al.actor_id;

CREATE VIEW v_applications_with_days_in_stage AS
SELECT
    a.*,
    TIMESTAMPDIFF(DAY, a.status_changed_at, NOW()) AS days_in_stage
FROM applications a;
