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

    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    verify_token        VARCHAR(255)    NULL,
    verify_token_expires_at TIMESTAMP   NULL,

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
-- 1. Xóa bảng vật lý trống do Hibernate tự tạo
DROP TABLE IF EXISTS v_activity_log_display CASCADE;

-- 2. Tạo lại View liên kết động đúng chuẩn đặc tả
CREATE VIEW v_activity_log_display AS
SELECT
    al.*,
    CASE
        WHEN u.status = 'INACTIVE' THEN al.actor_username || ' (deactivated)'
        ELSE al.actor_username
    END AS actor_display_name
FROM activity_log al
LEFT JOIN users u ON u.id = al.actor_id;


-- ============================================================
-- DEMO DATA SEEDING
-- ============================================================

-- Clean existing data first
TRUNCATE TABLE activity_log, interviews, application_notes, applications, job_postings, password_reset_tokens, users CASCADE;

-- Reset SERIAL sequences
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE job_postings_id_seq RESTART WITH 1;
ALTER SEQUENCE applications_id_seq RESTART WITH 1;
ALTER SEQUENCE interviews_id_seq RESTART WITH 1;
ALTER SEQUENCE activity_log_id_seq RESTART WITH 1;

-- 1. USERS
-- Password hash: '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe' encodes 'Admin@123'
INSERT INTO users (id, full_name, username, email, password_hash, role, status, failed_login_count, locked_at, email_verified, verify_token, verify_token_expires_at, created_at, updated_at) VALUES
(1, 'System Admin', 'admin', 'admin@talenthub.local', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'ADMIN', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(2, 'Alice Smith', 'alice_hr', 'alice@talenthub.local', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'HR_MANAGER', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(3, 'Bob Jones', 'bob_hr', 'bob@talenthub.local', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'HR_MANAGER', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(4, 'Charlie Brown', 'charlie_int', 'charlie@talenthub.local', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'INTERVIEWER', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(5, 'Diana Prince', 'diana_int', 'diana@talenthub.local', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'INTERVIEWER', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(6, 'John Doe', 'johndoe', 'johndoe@gmail.com', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'CANDIDATE', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(7, 'Jane Miller', 'janemiller', 'janemiller@gmail.com', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'CANDIDATE', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(8, 'Alex Johnson', 'alexj', 'alexj@gmail.com', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'CANDIDATE', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(9, 'Emma Watson', 'emmaw', 'emmaw@gmail.com', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'CANDIDATE', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW()),
(10, 'Ryan Reynolds', 'ryanr', 'ryanr@gmail.com', '$2a$10$sbUBO838c1mQz/BA2QcQ.OYTxQdf101srB5eVqMAr7cWBl6WGLFNe', 'CANDIDATE', 'ACTIVE', 0, NULL, TRUE, NULL, NULL, NOW(), NOW());

SELECT setval('users_id_seq', 10);

-- 2. JOB POSTINGS
INSERT INTO job_postings (id, title, department, location, description, requirements, salary_range, application_deadline, status, created_by, created_at, updated_at) VALUES
(1, 'Senior Java Developer', 'Engineering', 'Ho Chi Minh City', 'We are looking for a Senior Java Developer to join our core backend engineering team. You will build high-throughput, low-latency APIs and collaborate with product teams to design robust solutions.', 'Requirements:\n- 5+ years of experience with Java, Spring Boot, and Hibernate.\n- Strong understanding of SQL databases, especially PostgreSQL.\n- Experience with microservices architecture.', '25,000,000 - 45,000,000 VND', CURRENT_DATE + 30, 'ACTIVE', 2, NOW(), NOW()),
(2, 'HR Generalist', 'Human Resources', 'Hanoi', 'We are seeking an HR Generalist to manage end-to-end recruitment operations, employee engagement events, and assist in organizational development initiatives.', 'Requirements:\n- Bachelor''s degree in Human Resources, Business, or related fields.\n- 2+ years of generalist HR experience.\n- Excellent communication skills.', '15,000,000 - 22,000,000 VND', CURRENT_DATE + 30, 'ACTIVE', 3, NOW(), NOW()),
(3, 'Product Manager', 'Product', 'Ho Chi Minh City', 'This is a draft posting for a future product management opening in our e-commerce business line.', 'Requirements:\n- Experience managing agile development teams.\n- Strong data-driven decision-making skills.', 'Negotiable', CURRENT_DATE + 45, 'DRAFT', 2, NOW(), NOW()),
(4, 'Intern QA Engineer', 'Engineering', 'Da Nang', 'This position is now closed. It was aimed at recruiting junior talent for QA testing.', 'Requirements:\n- Basic programming knowledge.\n- High attention to detail.', '5,000,000 VND', CURRENT_DATE + 10, 'CLOSED', 3, NOW(), NOW());

SELECT setval('job_postings_id_seq', 4);

-- 3. APPLICATIONS
INSERT INTO applications (id, job_id, candidate_id, cover_letter, cv_filename, cv_storage_path, status, status_changed_at, submitted_at, updated_at) VALUES
(1, 1, 6, 'Hi, I am highly interested in the Java Developer role. Here is my resume.', 'john_doe_resume.pdf', 'uploads/john_doe_resume.pdf', 'APPLIED', NOW(), NOW(), NOW()),
(2, 1, 7, 'I would love to apply for this position and build awesome backend APIs.', 'jane_miller_resume.pdf', 'uploads/jane_miller_resume.pdf', 'SCREENING', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW()),
(3, 1, 8, 'Dear hiring team, I have 6 years of Java experience. Looking forward to your call.', 'alex_johnson_cv.pdf', 'uploads/alex_johnson_cv.pdf', 'INTERVIEW', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
(4, 1, 9, 'I am excited about this opportunity. Please find my CV attached.', 'emma_watson_cv.docx', 'uploads/emma_watson_cv.docx', 'OFFER', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW()),
(5, 1, 10, 'Hello, I believe I have the right skill set for this team. Please review my profile.', 'ryan_reynolds_resume.pdf', 'uploads/ryan_reynolds_resume.pdf', 'HIRED', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NOW()),
(6, 2, 6, 'I am also applying for HR Generalist position to help build great teams.', 'john_doe_hr_cv.pdf', 'uploads/john_doe_hr_cv.pdf', 'REJECTED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW()),
(7, 2, 7, 'Applying for the generalist position in Hanoi.', 'jane_miller_generalist_cv.docx', 'uploads/jane_miller_generalist_cv.docx', 'WITHDRAWN', NOW(), NOW() - INTERVAL '1 day', NOW());

SELECT setval('applications_id_seq', 7);

-- 4. APPLICATION NOTES
INSERT INTO application_notes (id, application_id, author_id, content, created_at) VALUES
(1, 2, 2, 'Candidate has solid experience in Java, but needs to prove microservice skills during the technical screen.', NOW() - INTERVAL '12 hours'),
(2, 3, 2, 'Alex showed excellent communication skills during HR screening. Invited to technical round.', NOW() - INTERVAL '1 day');

-- 5. INTERVIEWS
INSERT INTO interviews (id, application_id, interviewer_id, interview_date, interview_time, location_or_link, status, rating, feedback, evaluated_at, assigned_by, created_at, updated_at) VALUES
(1, 3, 4, CURRENT_DATE + 2, '10:00:00', 'https://meet.google.com/abc-defg-hij', 'SCHEDULED', NULL, NULL, NULL, 2, NOW(), NOW()),
(2, 5, 5, CURRENT_DATE, '09:00:00', 'https://meet.google.com/xyz-uvwx-yza', 'EVALUATED', 5, 'Ryan has outstanding problem-solving abilities and exceptional database knowledge. Clear hire.', NOW() - INTERVAL '1 hour', 2, NOW() - INTERVAL '4 days', NOW());

SELECT setval('interviews_id_seq', 2);

-- 6. ACTIVITY LOGS
INSERT INTO activity_log (id, actor_id, actor_username, event_type, description, ip_address, created_at) VALUES
(1, 1, 'admin', 'SIGN_IN_SUCCESS', 'Administrator logged in successfully.', '127.0.0.1'::inet, NOW() - INTERVAL '1 hour'),
(2, 2, 'alice_hr', 'ACCOUNT_CREATED', 'HR Manager account registered.', '127.0.0.1'::inet, NOW() - INTERVAL '5 days'),
(3, 6, 'johndoe', 'ACCOUNT_CREATED', 'Candidate profile registered.', '127.0.0.1'::inet, NOW() - INTERVAL '2 days');

SELECT setval('activity_log_id_seq', 3);
