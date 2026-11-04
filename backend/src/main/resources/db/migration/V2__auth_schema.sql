-- Reference data -----------------------------------------------------------
CREATE TABLE faculties (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       citext NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE departments (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       citext NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE study_groups (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       citext NOT NULL UNIQUE,
    faculty_id uuid NOT NULL REFERENCES faculties (id),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE disciplines (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name          citext NOT NULL,
    department_id uuid NOT NULL REFERENCES departments (id),
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (name, department_id)
);

CREATE TABLE semesters (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name       text NOT NULL UNIQUE,
    starts_on  date NOT NULL,
    ends_on    date NOT NULL,
    is_current boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_semesters_period CHECK (ends_on > starts_on)
);

CREATE UNIQUE INDEX ux_semesters_current ON semesters (is_current) WHERE is_current;

-- Users --------------------------------------------------------------------
CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         citext NOT NULL UNIQUE,
    password_hash text NOT NULL,
    full_name     text NOT NULL,
    role          varchar(32) NOT NULL,
    is_active     boolean NOT NULL DEFAULT true,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'TEACHER', 'ORGANIZER', 'ACADEMIC_OFFICE'))
);

CREATE TABLE student_profiles (
    user_id         uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    group_id        uuid NOT NULL REFERENCES study_groups (id),
    is_group_leader boolean NOT NULL DEFAULT false,
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_student_profiles_group ON student_profiles (group_id);

-- Teacher assignments ------------------------------------------------------
CREATE TABLE teacher_assignments (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id    uuid NOT NULL REFERENCES users (id),
    discipline_id uuid NOT NULL REFERENCES disciplines (id),
    group_id      uuid NOT NULL REFERENCES study_groups (id),
    semester_id   uuid NOT NULL REFERENCES semesters (id),
    status        varchar(16) NOT NULL DEFAULT 'ACTIVE',
    assigned_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_teacher_assignments_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    UNIQUE (teacher_id, discipline_id, group_id, semester_id)
);

CREATE INDEX ix_teacher_assignments_teacher ON teacher_assignments (teacher_id, semester_id);
CREATE INDEX ix_teacher_assignments_group ON teacher_assignments (group_id);

-- Teacher invites ----------------------------------------------------------
CREATE TABLE teacher_invites (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         citext NOT NULL,
    full_name     text NOT NULL,
    department_id uuid NOT NULL REFERENCES departments (id),
    token_hash    text NOT NULL UNIQUE,
    status        varchar(16) NOT NULL DEFAULT 'PENDING',
    expires_at    timestamptz NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    accepted_at   timestamptz,
    CONSTRAINT ck_teacher_invites_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

CREATE UNIQUE INDEX ux_teacher_invites_pending_email ON teacher_invites (email) WHERE status = 'PENDING';

CREATE TABLE teacher_invite_assignments (
    invite_id     uuid NOT NULL REFERENCES teacher_invites (id) ON DELETE CASCADE,
    discipline_id uuid NOT NULL REFERENCES disciplines (id),
    group_id      uuid NOT NULL REFERENCES study_groups (id),
    PRIMARY KEY (invite_id, discipline_id, group_id)
);

-- Student registration requests -------------------------------------------
CREATE TABLE registration_requests (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email             citext NOT NULL,
    full_name         text NOT NULL,
    password_hash     text,
    faculty_id        uuid NOT NULL REFERENCES faculties (id),
    group_id          uuid NOT NULL REFERENCES study_groups (id),
    status            varchar(24) NOT NULL DEFAULT 'PENDING_EMAIL',
    email_verified_at timestamptz,
    expires_at        timestamptz NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_registration_requests_status CHECK (
        status IN ('PENDING_EMAIL', 'EMAIL_VERIFIED', 'APPROVED', 'REJECTED', 'EXPIRED')
    )
);

CREATE UNIQUE INDEX ux_registration_requests_active_email
    ON registration_requests (email)
    WHERE status IN ('PENDING_EMAIL', 'EMAIL_VERIFIED');

CREATE INDEX ix_registration_requests_group_status ON registration_requests (group_id, status);
CREATE INDEX ix_registration_requests_status_expires ON registration_requests (status, expires_at);
