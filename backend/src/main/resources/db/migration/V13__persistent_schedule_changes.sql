ALTER TABLE campus_schedule_entries
    ADD COLUMN teacher_id uuid REFERENCES users(id);

CREATE INDEX ix_campus_schedule_teacher_weekday
    ON campus_schedule_entries(teacher_id, weekday, start_time, end_time);

CREATE TABLE campus_schedule_changes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
    schedule_entry_id uuid,
    change_type varchar(16) NOT NULL,
    message text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_campus_schedule_change_type CHECK (change_type IN ('ADDED', 'CHANGED', 'CANCELLED'))
);

CREATE INDEX ix_campus_schedule_changes_group_created
    ON campus_schedule_changes(group_id, created_at DESC);
