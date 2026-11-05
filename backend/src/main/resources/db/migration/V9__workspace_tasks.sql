CREATE TABLE workspace_tasks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id uuid NOT NULL REFERENCES users (id),
    group_id uuid NOT NULL REFERENCES study_groups (id),
    discipline_id uuid NOT NULL REFERENCES disciplines (id),
    title varchar(200) NOT NULL,
    description text NOT NULL,
    due_at timestamptz NOT NULL,
    max_points integer NOT NULL,
    allow_late_submission boolean NOT NULL DEFAULT false,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_workspace_tasks_max_points CHECK (max_points BETWEEN 0 AND 100)
);

CREATE INDEX ix_workspace_tasks_group_due ON workspace_tasks (group_id, due_at);
CREATE INDEX ix_workspace_tasks_teacher ON workspace_tasks (teacher_id);

CREATE TABLE workspace_submissions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id uuid NOT NULL REFERENCES workspace_tasks (id),
    student_id uuid NOT NULL REFERENCES users (id),
    submitted_at timestamptz NOT NULL DEFAULT now(),
    status varchar(16) NOT NULL,
    content text,
    attachment_name varchar(255),
    attachment_content_type varchar(120),
    attachment_data bytea,
    teacher_feedback text,
    grade integer,
    awarded_rating integer,
    revision_count integer NOT NULL DEFAULT 0,
    CONSTRAINT uk_workspace_submission_task_student UNIQUE (task_id, student_id),
    CONSTRAINT ck_workspace_submission_status CHECK (status IN ('SUBMITTED', 'RETURNED', 'ACCEPTED')),
    CONSTRAINT ck_workspace_submission_grade CHECK (grade IS NULL OR grade BETWEEN 2 AND 5)
);

CREATE INDEX ix_workspace_submissions_status ON workspace_submissions (status, submitted_at);
