ALTER TABLE campus_pass_reward_catalog
    ADD COLUMN case_id bigint REFERENCES cases(id);

UPDATE campus_pass_reward_catalog
SET case_id = (
    SELECT id FROM cases WHERE active = true AND name = 'Кейс кампуса' ORDER BY id LIMIT 1
)
WHERE reward_kind = 'case';

ALTER TABLE campus_pass_reward_catalog
    ADD CONSTRAINT ck_campus_pass_reward_case_selection CHECK (
        (reward_kind = 'case' AND case_id IS NOT NULL AND cosmetic_type IS NULL)
        OR (reward_kind = 'cosmetic' AND case_id IS NULL AND cosmetic_type IS NOT NULL)
    );
