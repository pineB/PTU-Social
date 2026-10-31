-- Student certificate requests ----------------------------------------------
CREATE TABLE certificate_requests (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id       uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type             varchar(32) NOT NULL,
    status           varchar(32) NOT NULL DEFAULT 'PENDING',
    rejection_reason text,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_certificate_requests_type CHECK (type IN ('EDUCATION', 'INCOME', 'MILITARY')),
    CONSTRAINT ck_certificate_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX ix_certificate_requests_student ON certificate_requests (student_id);
CREATE INDEX ix_certificate_requests_status ON certificate_requests (status);
