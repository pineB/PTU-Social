CREATE TABLE campus_pass_settings (
    id smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    season_name varchar(120) NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    CONSTRAINT ck_campus_pass_season_dates CHECK (ends_at > starts_at)
);

INSERT INTO campus_pass_settings(id, season_name, starts_at, ends_at)
VALUES (1, 'ПТУ-пасс', now(), now() + interval '1 year');

CREATE TABLE campus_pass_seasons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    season_name varchar(120) NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    premium_pass_price numeric(19,2) NOT NULL CHECK (premium_pass_price > 0),
    is_active boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_campus_pass_season_dates CHECK (ends_at > starts_at)
);

INSERT INTO campus_pass_seasons(season_name, starts_at, ends_at, premium_pass_price, is_active)
SELECT s.season_name, s.starts_at, s.ends_at, c.premium_pass_price, true
FROM campus_pass_settings s
JOIN campus_runtime_catalog_config c ON c.id = s.id
WHERE s.id = 1;

CREATE UNIQUE INDEX ux_campus_pass_one_active_season
    ON campus_pass_seasons(is_active) WHERE is_active;

ALTER TABLE campus_pass_reward_catalog
    ADD COLUMN season_id uuid REFERENCES campus_pass_seasons(id);

UPDATE campus_pass_reward_catalog
SET season_id = (SELECT id FROM campus_pass_seasons WHERE is_active);

ALTER TABLE campus_pass_reward_catalog
    ALTER COLUMN season_id SET NOT NULL,
    DROP CONSTRAINT ux_campus_pass_track_level,
    ADD CONSTRAINT ux_campus_pass_season_track_level UNIQUE(season_id, track, level);

ALTER TABLE campus_pass_accounts
    ADD COLUMN season_id uuid REFERENCES campus_pass_seasons(id);

UPDATE campus_pass_accounts
SET season_id = (SELECT id FROM campus_pass_seasons WHERE is_active);

ALTER TABLE campus_pass_accounts
    ALTER COLUMN season_id SET NOT NULL,
    DROP CONSTRAINT campus_pass_accounts_pkey,
    ADD CONSTRAINT campus_pass_accounts_pkey PRIMARY KEY(user_id, season_id);

ALTER TABLE workspace_tasks
    ADD COLUMN attachment_name varchar(255),
    ADD COLUMN attachment_content_type varchar(120),
    ADD COLUMN attachment_data bytea;

ALTER TABLE workspace_submissions
    ADD COLUMN awarded_at timestamptz;

ALTER TABLE campus_schedule_entries
    ADD COLUMN discipline_id uuid REFERENCES disciplines(id);

CREATE INDEX ix_campus_schedule_discipline
    ON campus_schedule_entries(discipline_id);
