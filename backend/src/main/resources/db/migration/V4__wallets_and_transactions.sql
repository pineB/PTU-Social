-- Wallets (internal currency) and transactions -------------------------------
CREATE TABLE wallets (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          uuid NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    currency_balance numeric(19, 2) NOT NULL DEFAULT 0,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_wallets_balance_non_negative CHECK (currency_balance >= 0)
);

CREATE TABLE transactions (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       uuid NOT NULL REFERENCES wallets (id) ON DELETE CASCADE,
    amount          numeric(19, 2) NOT NULL,
    type            varchar(32) NOT NULL,
    idempotency_key varchar(128) NOT NULL UNIQUE,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (type IN ('REVIEW_REWARD', 'MONTHLY_CONVERSION', 'ADJUSTMENT'))
);

CREATE INDEX ix_transactions_wallet_created ON transactions (wallet_id, created_at);
