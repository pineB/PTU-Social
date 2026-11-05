ALTER TABLE activities
    ADD COLUMN location varchar(255) NOT NULL DEFAULT '',
    ADD COLUMN category varchar(120) NOT NULL DEFAULT 'Событие',
    ADD COLUMN campus_activity_type varchar(8) NOT NULL DEFAULT 'БОА',
    ADD COLUMN max_points integer NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_activities_campus_type CHECK (campus_activity_type IN ('БОА', 'БУА')),
    ADD CONSTRAINT ck_activities_max_points CHECK (max_points BETWEEN 0 AND 100);

ALTER TABLE participations
    ADD COLUMN group_registration_id uuid,
    ADD COLUMN participation_confirmed boolean NOT NULL DEFAULT false,
    ADD COLUMN awarded_percent integer,
    ADD COLUMN awarded_at timestamp;

CREATE TABLE campus_event_group_registrations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id bigint NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    group_id uuid NOT NULL REFERENCES study_groups(id),
    added_by uuid NOT NULL REFERENCES users(id),
    group_size integer NOT NULL CHECK (group_size > 0),
    requested_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_campus_event_group UNIQUE(activity_id, group_id)
);

ALTER TABLE participations
    ADD CONSTRAINT fk_participation_group_registration
        FOREIGN KEY (group_registration_id) REFERENCES campus_event_group_registrations(id);

CREATE INDEX ix_campus_event_groups_activity ON campus_event_group_registrations(activity_id);
CREATE INDEX ix_participations_group_registration ON participations(group_registration_id);

CREATE UNIQUE INDEX ux_reviews_activity_author ON reviews(activity_id, author_id);

CREATE TABLE campus_group_confirmations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id uuid NOT NULL REFERENCES study_groups(id),
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    submitted_at timestamptz NOT NULL DEFAULT now(),
    reviewed_by uuid REFERENCES users(id),
    reviewed_at timestamptz,
    CONSTRAINT ck_campus_group_confirmation_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX ix_campus_group_confirmations_group_status
    ON campus_group_confirmations(group_id, status, submitted_at);
CREATE UNIQUE INDEX ux_campus_group_confirmation_pending
    ON campus_group_confirmations(student_id, group_id) WHERE status = 'PENDING';

WITH ranked_leaders AS (
    SELECT user_id, row_number() OVER (PARTITION BY group_id ORDER BY updated_at DESC, user_id) AS rank
    FROM student_profiles
    WHERE is_group_leader = true
)
UPDATE student_profiles sp
SET is_group_leader = false
FROM ranked_leaders leaders
WHERE sp.user_id = leaders.user_id AND leaders.rank > 1;

CREATE UNIQUE INDEX ux_student_profiles_group_leader
    ON student_profiles(group_id) WHERE is_group_leader = true;

CREATE TABLE campus_schedule_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL REFERENCES study_groups(id),
    weekday varchar(24) NOT NULL,
    start_time varchar(5) NOT NULL,
    end_time varchar(5) NOT NULL,
    subject varchar(160) NOT NULL,
    teacher varchar(160) NOT NULL,
    room varchar(80) NOT NULL,
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_campus_schedule_group_time
    ON campus_schedule_entries(group_id, weekday, start_time);

CREATE TABLE campus_case_reward_catalog (
    id bigserial PRIMARY KEY,
    case_id bigint NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    reward_name varchar(120) NOT NULL,
    cosmetic_type varchar(32) NOT NULL,
    rarity varchar(16) NOT NULL,
    weight integer NOT NULL,
    CONSTRAINT ck_campus_case_reward_weight CHECK (weight > 0),
    CONSTRAINT ck_campus_case_reward_rarity CHECK (rarity IN ('Обычный', 'Редкий', 'Эпический')),
    CONSTRAINT ck_campus_case_cosmetic_type CHECK (cosmetic_type IN ('avatar', 'avatar-frame', 'profile-background', 'badge'))
);
CREATE INDEX ix_campus_case_reward_catalog_case
    ON campus_case_reward_catalog(case_id);

INSERT INTO cases(name, description, rarity, price, active)
VALUES ('Кейс кампуса', 'Награды кампуса', 'COMMON', 100, true);

INSERT INTO campus_case_reward_catalog(case_id, reward_name, cosmetic_type, rarity, weight)
SELECT id, 'Аватар «Старт»', 'avatar', 'Обычный', 55 FROM cases WHERE name = 'Кейс кампуса';
INSERT INTO campus_case_reward_catalog(case_id, reward_name, cosmetic_type, rarity, weight)
SELECT id, 'Рамка «Студент»', 'avatar-frame', 'Обычный', 30 FROM cases WHERE name = 'Кейс кампуса';
INSERT INTO campus_case_reward_catalog(case_id, reward_name, cosmetic_type, rarity, weight)
SELECT id, 'Значок «Активист»', 'badge', 'Редкий', 12 FROM cases WHERE name = 'Кейс кампуса';
INSERT INTO campus_case_reward_catalog(case_id, reward_name, cosmetic_type, rarity, weight)
SELECT id, 'Фон «Кампус»', 'profile-background', 'Эпический', 3 FROM cases WHERE name = 'Кейс кампуса';

CREATE TABLE campus_pass_reward_catalog (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    track varchar(12) NOT NULL,
    level integer NOT NULL,
    title varchar(120) NOT NULL,
    reward_kind varchar(16) NOT NULL,
    cosmetic_type varchar(32),
    rarity varchar(16) NOT NULL DEFAULT 'Обычный',
    CONSTRAINT ck_campus_pass_track CHECK (track IN ('free', 'premium')),
    CONSTRAINT ck_campus_pass_reward_kind CHECK (reward_kind IN ('case', 'cosmetic')),
    CONSTRAINT ck_campus_pass_reward_level CHECK (level BETWEEN 1 AND 50),
    CONSTRAINT ux_campus_pass_track_level UNIQUE(track, level)
);

INSERT INTO campus_pass_reward_catalog(track, level, title, reward_kind, cosmetic_type, rarity)
SELECT CASE WHEN mod(level, 5) = 0 THEN 'premium' ELSE 'free' END,
       level,
       CASE WHEN mod(level, 5) = 0 THEN 'Награда уровня ' || level ELSE 'Кейс уровня ' || level END,
       CASE WHEN mod(level, 5) = 0 THEN 'cosmetic' ELSE 'case' END,
       CASE WHEN mod(level, 5) = 0 THEN 'badge' ELSE NULL END,
       CASE WHEN mod(level, 5) = 0 THEN 'Редкий' ELSE 'Обычный' END
FROM generate_series(1, 50) AS levels(level);

CREATE TABLE campus_pass_accounts (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    premium_pass_owned boolean NOT NULL DEFAULT false,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE campus_pass_reward_claims (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reward_id uuid NOT NULL REFERENCES campus_pass_reward_catalog(id),
    claimed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_campus_pass_claim_user_reward UNIQUE(user_id, reward_id)
);

CREATE TABLE campus_runtime_catalog_config (
    id smallint PRIMARY KEY DEFAULT 1 CHECK(id = 1),
    premium_pass_price numeric(19,2) NOT NULL CHECK(premium_pass_price > 0)
);

INSERT INTO campus_runtime_catalog_config(id, premium_pass_price) VALUES(1, 250);

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_type CHECK (
    type IN ('REVIEW_REWARD', 'MONTHLY_CONVERSION', 'ADJUSTMENT', 'CASE_OPEN', 'PASS_PREMIUM')
);

ALTER TABLE inventory_items
    ADD COLUMN item_kind varchar(16) NOT NULL DEFAULT 'case',
    ADD COLUMN cosmetic_type varchar(32),
    ADD COLUMN source varchar(160) NOT NULL DEFAULT 'Кейс',
    ADD CONSTRAINT ck_inventory_item_kind CHECK (item_kind IN ('case', 'cosmetic'));
