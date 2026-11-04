-- Test-only stub for the `reviews` table owned by the activities/reviews module.
-- Minimal contract used by ScheduledTasks: id, author_id (UUID!), status, reward_awarded.
-- Remove once the real migration from the reviews module is in the project.
CREATE TABLE IF NOT EXISTS reviews (
    id             bigserial PRIMARY KEY,
    activity_id    bigint,
    author_id      uuid NOT NULL,
    text           varchar(2000),
    rating         integer,
    status         varchar(30) NOT NULL DEFAULT 'PENDING_MODERATION',
    moderated_by   uuid,
    reward_awarded boolean NOT NULL DEFAULT false,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);
