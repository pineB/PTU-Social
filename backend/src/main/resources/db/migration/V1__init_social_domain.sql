CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL,
    participation_mode VARCHAR(30) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    max_participants INTEGER NOT NULL,
    min_participants INTEGER NOT NULL,
    required_activation_percent INTEGER NOT NULL,
    reward_boa DECIMAL(5,2) NOT NULL DEFAULT 0,
    reward_bua DECIMAL(5,2) NOT NULL DEFAULT 0,
    teacher_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE participations (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    points_boa DECIMAL(5,2) DEFAULT 0,
    points_bua DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_participation_activity_student UNIQUE (activity_id, student_id)
);

CREATE TABLE teacher_budgets (
    id BIGSERIAL PRIMARY KEY,
    teacher_id UUID NOT NULL UNIQUE,
    total_budget DECIMAL(19,2) NOT NULL DEFAULT 0,
    spent_budget DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    author_id UUID NOT NULL,
    text TEXT NOT NULL,
    rating INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_MODERATION',
    moderated_by UUID,
    reward_awarded BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE battle_pass_seasons (
    id BIGSERIAL PRIMARY KEY,
    student_id UUID NOT NULL,
    season_name VARCHAR(120) NOT NULL,
    current_level INTEGER NOT NULL DEFAULT 1,
    total_action_points INTEGER NOT NULL DEFAULT 0,
    premium_track_purchased BOOLEAN NOT NULL DEFAULT FALSE,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE levels (
    id BIGSERIAL PRIMARY KEY,
    battle_pass_season_id BIGINT NOT NULL,
    level_number INTEGER NOT NULL,
    required_xp INTEGER NOT NULL,
    reward_currency INTEGER NOT NULL,
    reward_case_id BIGINT,
    reward_name VARCHAR(120),
    CONSTRAINT fk_level_battle_pass FOREIGN KEY (battle_pass_season_id) REFERENCES battle_pass_seasons (id)
);

CREATE TABLE cases (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    rarity VARCHAR(30) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    owner_student_id UUID NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    rarity VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_case FOREIGN KEY (case_id) REFERENCES cases (id)
);

CREATE INDEX idx_activity_teacher ON activities (teacher_id);
CREATE INDEX idx_participation_activity ON participations (activity_id);
CREATE INDEX idx_participation_student ON participations (student_id);
CREATE INDEX idx_review_activity ON reviews (activity_id);
CREATE INDEX idx_review_author ON reviews (author_id);
CREATE INDEX idx_review_status ON reviews (status);
CREATE INDEX idx_level_battle_pass ON levels (battle_pass_season_id);
CREATE INDEX idx_inventory_owner ON inventory_items (owner_student_id);
CREATE INDEX idx_inventory_case ON inventory_items (case_id);
