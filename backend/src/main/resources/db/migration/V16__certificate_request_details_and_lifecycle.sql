ALTER TABLE certificate_requests
    DROP CONSTRAINT ck_certificate_requests_type,
    DROP CONSTRAINT ck_certificate_requests_status,
    ADD COLUMN custom_title varchar(120),
    ADD COLUMN description text NOT NULL DEFAULT '',
    ADD COLUMN delivery_method varchar(24) NOT NULL DEFAULT 'PICKUP';

UPDATE certificate_requests SET status = 'READY' WHERE status = 'APPROVED';

ALTER TABLE certificate_requests
    ADD CONSTRAINT ck_certificate_requests_type
        CHECK (type IN ('EDUCATION', 'INCOME', 'MILITARY', 'OTHER')),
    ADD CONSTRAINT ck_certificate_requests_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'READY', 'APPROVED', 'ISSUED', 'REJECTED')),
    ADD CONSTRAINT ck_certificate_delivery_method
        CHECK (delivery_method IN ('PICKUP', 'DIGITAL')),
    ADD CONSTRAINT ck_certificate_custom_title
        CHECK (type <> 'OTHER' OR custom_title IS NOT NULL AND length(trim(custom_title)) > 0);
