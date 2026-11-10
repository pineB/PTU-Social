ALTER TABLE cases
    ADD COLUMN image_data bytea,
    ADD COLUMN image_content_type varchar(100);

ALTER TABLE campus_case_reward_catalog
    ADD COLUMN image_data bytea,
    ADD COLUMN image_content_type varchar(100);

ALTER TABLE inventory_items
    ADD COLUMN image_data bytea,
    ADD COLUMN image_content_type varchar(100);

ALTER TABLE student_profiles
    ADD COLUMN equipped_avatar_item_id bigint REFERENCES inventory_items(id) ON DELETE SET NULL,
    ADD COLUMN equipped_frame_item_id bigint REFERENCES inventory_items(id) ON DELETE SET NULL,
    ADD COLUMN equipped_background_item_id bigint REFERENCES inventory_items(id) ON DELETE SET NULL;
