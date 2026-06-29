-- ============================================================
-- TalentHub Recruitment Management System
-- Database Schema (PostgreSQL)
-- Derived strictly from Group_Project.pdf
-- ============================================================

-- ============================================================
-- ENUMS
-- ============================================================

CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'HR_MANAGER',
    'INTERVIEWER',
    'CANDIDATE'
);

CREATE TYPE user_status AS ENUM (
    'ACTIVE',
    'LOCKED',
    'INACTIVE'
);

CREATE TYPE job_status AS ENUM (
    'DRAFT',
    'ACTIVE',
    'CLOSED'
);

CREATE TYPE application_status AS ENUM (
    'APPLIED',
    'SCREENING',
    'INTERVIEW',
    'OFFER',
    'HIRED',
    'REJECTED',
    'WITHDRAWN'
);

CREATE TYPE interview_status AS ENUM (
    'SCHEDULED',
    'EVALUATED'
);

CREATE TYPE activity_event_type AS ENUM (
    'SIGN_IN_SUCCESS',
    'SIGN_IN_FAILURE',
    'ACCOUNT_CREATED',
    'ACCOUNT_DEACTIVATED',
    'ACCOUNT_UNLOCKED',
    'ACCOUNT_LOCKED',
    'APPLICATION_STATUS_CHANGED',
    'CV_DOWNLOADED',
    'EVALUATION_SUBMITTED'
);

-- ============================================================
-- 1. USERS
-- Covers: Admin, HR Manager, Interviewer, Candidate (all
-- authenticated roles). Guests are unauthenticated – no row.
--
-- Sources:
--   SCR-03 (register): full_name, username, email, password
--   SCR-05 (profile):  role badge, read-only display
--   SCR-08 (user mgmt): role, status, date_created
--   1.2 Actor List:    role hierarchy, creation rules
-- ============================================================

CREATE TABLE users (
    id                  SERIAL          PRIMARY KEY,

    -- SCR-03 / SCR-08: required registration fields
    full_name           VARCHAR(255)    NOT NULL,
    username            VARCHAR(50)     NOT NULL UNIQUE,   -- 4-50 chars, letters/digits/underscores
    email               VARCHAR(255)    NOT NULL UNIQUE,

    -- Hashed password. SCR-03 complexity: ≥8 chars, 1 uppercase, 1 number
    password_hash       VARCHAR(255)    NOT NULL,

    role                user_role       NOT NULL,
    status              user_status     NOT NULL DEFAULT 'ACTIVE',

    -- Lockout: SCR-01 "locked after too many failed attempts"
    failed_login_count  SMALLINT        NOT NULL DEFAULT 0,
    locked_at           TIMESTAMPTZ     NULL,           -- when account was locked

    -- SCR-08 user list: date_created column
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- SCR-08: Admin cannot be created via UI; only via initial setup
    -- CONSTRAINT: Admin role cannot self-register (enforced at app layer)

    CONSTRAINT chk_username_format CHECK (
        username ~ '^[A-Za-z0-9_]{4,50}$'
    )
);

-- ============================================================
-- 2. PASSWORD_RESET_TOKENS
-- SCR-02: one-time token sent via email; expires after use
-- ============================================================

CREATE TABLE password_reset_tokens (
    id          SERIAL          PRIMARY KEY,
    user_id     INT             NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255)    NOT NULL UNIQUE,     -- secure random token
    expires_at  TIMESTAMPTZ     NOT NULL,
    used_at     TIMESTAMPTZ     NULL,                -- NULL = not yet used
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 3. JOB_POSTINGS
-- SCR-11 (Job Form), SCR-12 (Job Detail), SCR-10 (Job List)
-- SCR-13 / SCR-14 (Public side)
-- ============================================================

CREATE TABLE job_postings (
    id                  SERIAL          PRIMARY KEY,

    -- SCR-11 required fields
    title               VARCHAR(200)    NOT NULL,
    department          VARCHAR(100)    NOT NULL,
    location            VARCHAR(100)    NOT NULL,
    description         TEXT            NOT NULL,

    -- SCR-11 optional fields
    requirements        TEXT            NULL,
    salary_range        VARCHAR(100)    NULL,       -- free text e.g. "15–20M VND"
    application_deadline DATE           NULL,       -- must be today or future if set

    status              job_status      NOT NULL DEFAULT 'DRAFT',

    -- SCR-12 "created by" display; SCR-10 HR Manager scope
    created_by          INT             NOT NULL REFERENCES users(id),

    -- SCR-12: date created shown in job info panel
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Derived: SCR-10 delete button hidden when application_count > 0
    -- application count is computed via JOIN – not stored here

    CONSTRAINT chk_deadline_future CHECK (
        application_deadline IS NULL OR application_deadline >= CURRENT_DATE
    )
);

-- ============================================================
-- 4. APPLICATIONS
-- SCR-14 (apply), SCR-15 (My Applications), SCR-16 (App List)
-- SCR-17 (App Detail)
-- ============================================================

CREATE TABLE applications (
    id              SERIAL              PRIMARY KEY,

    job_id          INT                 NOT NULL REFERENCES job_postings(id),
    candidate_id    INT                 NOT NULL REFERENCES users(id),  -- role = CANDIDATE

    -- SCR-14: cover letter optional, CV required (PDF/DOCX, max 5 MB)
    cover_letter    TEXT                NULL,
    cv_filename     VARCHAR(255)        NOT NULL,   -- original filename shown to HR
    cv_storage_path VARCHAR(500)        NOT NULL,   -- server-side storage path / S3 key

    status          application_status  NOT NULL DEFAULT 'APPLIED',

    -- SCR-16: "days in current stage" → derived from status_changed_at
    status_changed_at TIMESTAMPTZ       NOT NULL DEFAULT NOW(),

    -- SCR-15: submitted date column
    submitted_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    -- SCR-14 / SCR-15: one application per candidate per job
    -- re-application blocked even after WITHDRAWN
    CONSTRAINT uq_application UNIQUE (job_id, candidate_id)
);

-- ============================================================
-- 5. APPLICATION_NOTES  (Internal HR notes)
-- SCR-17: "Add Note" – HR Manager / Admin only
-- Notes are permanent and read-only after creation (no update/delete)
-- Interviewer NEVER sees these (hidden entirely)
-- ============================================================

CREATE TABLE application_notes (
    id              SERIAL          PRIMARY KEY,
    application_id  INT             NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    author_id       INT             NOT NULL REFERENCES users(id),   -- HR Manager or Admin
    content         TEXT            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
    -- No updated_at: notes are immutable after creation (per SCR-17)
);

-- ============================================================
-- 6. INTERVIEWS
-- SCR-18 (Interview Assignment), SCR-19 (Evaluation Form)
-- Multiple interview rounds allowed per application
-- ============================================================

CREATE TABLE interviews (
    id                  SERIAL              PRIMARY KEY,
    application_id      INT                 NOT NULL REFERENCES applications(id) ON DELETE CASCADE,

    -- SCR-18: Interviewer selector – active INTERVIEWER accounts only
    interviewer_id      INT                 NOT NULL REFERENCES users(id),   -- role = INTERVIEWER

    -- SCR-18: date + time fields
    interview_date      DATE                NOT NULL,
    interview_time      TIME                NOT NULL,               -- HH:mm 24-hour

    -- SCR-18: optional room name or meeting URL (max 500 chars)
    location_or_link    VARCHAR(500)        NULL,

    status              interview_status    NOT NULL DEFAULT 'SCHEDULED',

    -- SCR-19: rating 1–5, written feedback; immutable after submit
    rating              SMALLINT            NULL CHECK (rating BETWEEN 1 AND 5),
    feedback            TEXT                NULL,
    evaluated_at        TIMESTAMPTZ         NULL,   -- set when Interviewer submits

    -- SCR-18: assigned by HR Manager or Admin
    assigned_by         INT                 NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    -- rating + feedback must both be present when status = EVALUATED
    CONSTRAINT chk_evaluated_fields CHECK (
        status = 'SCHEDULED'
        OR (status = 'EVALUATED' AND rating IS NOT NULL AND feedback IS NOT NULL AND evaluated_at IS NOT NULL)
    ),

    -- interview must be scheduled for a future date (enforced at app layer too)
    CONSTRAINT chk_interview_future CHECK (
        interview_date >= CURRENT_DATE
    )
);

-- ============================================================
-- 7. ACTIVITY_LOG  (Audit log)
-- SCR-09: searchable, filterable, chronological, read-only
-- SCR-07: last 10 events shown on Admin Dashboard
-- Events: sign-in, account changes, application status, CV download, evaluation
-- ============================================================

CREATE TABLE activity_log (
    id              BIGSERIAL           PRIMARY KEY,

    -- SCR-09: Actor (username). Preserved even if user later deactivated.
    actor_id        INT                 NULL REFERENCES users(id) ON DELETE SET NULL,
    actor_username  VARCHAR(50)         NOT NULL,   -- snapshot at time of event

    event_type      activity_event_type NOT NULL,

    -- SCR-09: Description/detail column – free-text context
    description     TEXT                NULL,

    -- SCR-09: IP address column
    ip_address      INET                NULL,

    -- SCR-09: Timestamp column; newest first
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW()

    -- No update or delete – log is permanently read-only (per SCR-09)
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Users
CREATE INDEX idx_users_role           ON users(role);
CREATE INDEX idx_users_status         ON users(status);
CREATE INDEX idx_users_email          ON users(email);

-- Job postings
CREATE INDEX idx_jobs_status          ON job_postings(status);
CREATE INDEX idx_jobs_created_by      ON job_postings(created_by);
CREATE INDEX idx_jobs_department      ON job_postings(department);

-- Applications
CREATE INDEX idx_apps_job_id          ON applications(job_id);
CREATE INDEX idx_apps_candidate_id    ON applications(candidate_id);
CREATE INDEX idx_apps_status          ON applications(status);
CREATE INDEX idx_apps_submitted_at    ON applications(submitted_at);

-- Application notes
CREATE INDEX idx_notes_application_id ON application_notes(application_id);

-- Interviews
CREATE INDEX idx_interviews_app_id       ON interviews(application_id);
CREATE INDEX idx_interviews_interviewer  ON interviews(interviewer_id);
CREATE INDEX idx_interviews_status       ON interviews(status);
CREATE INDEX idx_interviews_date         ON interviews(interview_date);

-- Activity log
CREATE INDEX idx_log_event_type    ON activity_log(event_type);
CREATE INDEX idx_log_actor_id      ON activity_log(actor_id);
CREATE INDEX idx_log_created_at    ON activity_log(created_at DESC);

-- Password reset
CREATE INDEX idx_prt_user_id       ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_token         ON password_reset_tokens(token);

-- ============================================================
-- VIEWS (convenience)
-- ============================================================

-- SCR-06 / SCR-07: application count per job (used in many places)
CREATE VIEW v_job_application_counts AS
SELECT
    job_id,
    COUNT(*)                                            AS total,
    COUNT(*) FILTER (WHERE status = 'APPLIED')          AS applied,
    COUNT(*) FILTER (WHERE status = 'SCREENING')        AS screening,
    COUNT(*) FILTER (WHERE status = 'INTERVIEW')        AS interview,
    COUNT(*) FILTER (WHERE status = 'OFFER')            AS offer,
    COUNT(*) FILTER (WHERE status = 'HIRED')            AS hired,
    COUNT(*) FILTER (WHERE status = 'REJECTED')         AS rejected,
    COUNT(*) FILTER (WHERE status = 'WITHDRAWN')        AS withdrawn
FROM applications
GROUP BY job_id;

-- SCR-09: deactivated actor display "[username] (deactivated)"
CREATE VIEW v_activity_log_display AS
SELECT
    al.*,
    CASE
        WHEN u.status = 'INACTIVE' THEN al.actor_username || ' (deactivated)'
        ELSE al.actor_username
    END AS actor_display_name
FROM activity_log al
LEFT JOIN users u ON u.id = al.actor_id;

-- SCR-16: days in current stage
CREATE VIEW v_applications_with_days_in_stage AS
SELECT
    a.*,
    EXTRACT(DAY FROM NOW() - a.status_changed_at)::INT AS days_in_stage
FROM applications a;

-- ============================================================
-- COMMENTS (documentation)
-- ============================================================

COMMENT ON TABLE users                  IS 'All authenticated actors: Admin, HR_MANAGER, INTERVIEWER, CANDIDATE. Guests are unauthenticated – no row.';
COMMENT ON TABLE password_reset_tokens  IS 'SCR-02: one-time tokens for password reset flow. Used_at marks consumption.';
COMMENT ON TABLE job_postings           IS 'SCR-10/11/12: job postings with DRAFT→ACTIVE→CLOSED lifecycle.';
COMMENT ON TABLE applications           IS 'SCR-14/15/16/17: candidate applications. One per (job, candidate). Re-apply after WITHDRAWN is blocked (UNIQUE constraint).';
COMMENT ON TABLE application_notes      IS 'SCR-17: internal HR notes. Immutable after creation. Hidden from Interviewers.';
COMMENT ON TABLE interviews             IS 'SCR-18/19: interview assignments and evaluations. Multiple rounds allowed per application.';
COMMENT ON TABLE activity_log           IS 'SCR-09: append-only audit log. Actor username snapshot preserved even after deactivation.';
