-- Migration: wallet / settlement (account_balances + trades + order lock columns)
-- Chạy: psql -U postgres -d account_demo -f scripts/migrate-wallet-settlement.sql

CREATE TABLE IF NOT EXISTS account_balances (
    id               BIGSERIAL PRIMARY KEY,
    account_id       VARCHAR(64) NOT NULL REFERENCES accounts(id),
    currency         VARCHAR(16) NOT NULL,
    available_amount BIGINT NOT NULL DEFAULT 0,
    locked_amount    BIGINT NOT NULL DEFAULT 0,
    deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by       VARCHAR(100),
    CONSTRAINT uq_account_balances_account_currency UNIQUE (account_id, currency)
);

-- Copy số dư cũ (nếu chưa có row)
INSERT INTO account_balances (account_id, currency, available_amount, locked_amount, deleted, created_at, updated_at)
SELECT a.id, COALESCE(a.balance_currency, 'VND'), COALESCE(a.balance_amount, 0), 0, false, NOW(), NOW()
FROM accounts a
WHERE NOT EXISTS (
    SELECT 1 FROM account_balances b WHERE b.account_id = a.id AND b.currency = COALESCE(a.balance_currency, 'VND')
);

-- Seed BTC demo nếu chưa có
INSERT INTO account_balances (account_id, currency, available_amount, locked_amount, deleted, created_at, updated_at)
SELECT id, 'BTC', 5, 0, false, NOW(), NOW()
FROM accounts
WHERE id IN ('ACC-001', 'ACC-002')
ON CONFLICT (account_id, currency) DO NOTHING;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS locked_currency VARCHAR(16);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS locked_amount_remaining BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS trades (
    id                 VARCHAR(64) PRIMARY KEY,
    buy_order_id       VARCHAR(64) NOT NULL,
    sell_order_id      VARCHAR(64) NOT NULL,
    buyer_account_id   VARCHAR(64) NOT NULL,
    seller_account_id  VARCHAR(64) NOT NULL,
    base_currency      VARCHAR(16) NOT NULL,
    quote_currency     VARCHAR(16) NOT NULL,
    quantity           BIGINT NOT NULL,
    price              BIGINT NOT NULL,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by         VARCHAR(100)
);

-- Trader2 → ACC-002 (nếu chưa có)
INSERT INTO users (username, password_hash, email, account_id, enabled, deleted, created_at, updated_at)
SELECT 'trader2',
       '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq',
       'trader2@example.com',
       'ACC-002',
       true, false, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'trader2');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username = 'trader2'
ON CONFLICT DO NOTHING;
