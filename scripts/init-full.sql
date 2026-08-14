-- =============================================================================
-- init-full.sql — Schema + seed đầy đủ cho demo Bank + Exchange
-- =============================================================================
-- Chạy (đã có database account_demo):
--   psql -U postgres -d account_demo -f scripts/init-full.sql
--
-- Chưa có DB:
--   psql -U postgres -c "CREATE DATABASE account_demo;"
--   psql -U postgres -d account_demo -f scripts/init-full.sql
--
-- Hoặc DBeaver: connect vào account_demo → Execute script này.
--
-- Password mẫu (BCrypt): password123
-- Users: admin / trader1(ACC-001) / trader2(ACC-002) / readonly1
-- =============================================================================

BEGIN;

-- ─── Drop tables (reset sạch) ────────────────────────────────────────────────
DROP TABLE IF EXISTS login_logs CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS user_permissions CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS resources CASCADE;
DROP TABLE IF EXISTS trades CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS order_books CASCADE;
DROP TABLE IF EXISTS account_balances CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;

-- ─── Account / Wallet ────────────────────────────────────────────────────────
CREATE TABLE accounts (
    id          VARCHAR(255) PRIMARY KEY,
    status      VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(255)
);

CREATE TABLE account_balances (
    id               BIGSERIAL PRIMARY KEY,
    account_id       VARCHAR(255) NOT NULL REFERENCES accounts(id),
    currency         VARCHAR(255),
    available_amount BIGINT       NOT NULL DEFAULT 0,
    locked_amount    BIGINT       NOT NULL DEFAULT 0,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by       VARCHAR(255),
    CONSTRAINT uk_account_balances_account_currency UNIQUE (account_id, currency)
);

-- ─── Exchange ────────────────────────────────────────────────────────────────
CREATE TABLE order_books (
    id              VARCHAR(255) PRIMARY KEY,
    base_currency   VARCHAR(255),
    quote_currency  VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(255)
);

CREATE TABLE orders (
    id                       VARCHAR(255) PRIMARY KEY,
    account_id               VARCHAR(255),
    side                     VARCHAR(255),
    order_type               VARCHAR(255),
    base_currency            VARCHAR(255),
    quote_currency           VARCHAR(255),
    quantity                 BIGINT       NOT NULL DEFAULT 0,
    price                    BIGINT,
    filled_quantity          BIGINT       NOT NULL DEFAULT 0,
    status                   VARCHAR(255),
    locked_currency          VARCHAR(255),
    locked_amount_remaining  BIGINT       NOT NULL DEFAULT 0,
    deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by               VARCHAR(255),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by               VARCHAR(255)
);

CREATE TABLE trades (
    id                  VARCHAR(255) PRIMARY KEY,
    buy_order_id        VARCHAR(255) NOT NULL,
    sell_order_id       VARCHAR(255) NOT NULL,
    buyer_account_id    VARCHAR(255) NOT NULL,
    seller_account_id   VARCHAR(255) NOT NULL,
    base_currency       VARCHAR(255) NOT NULL,
    quote_currency      VARCHAR(255) NOT NULL,
    quantity            BIGINT       NOT NULL,
    price               BIGINT       NOT NULL,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(255)
);

-- ─── Security / RBAC ─────────────────────────────────────────────────────────
CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(255)
);

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(150) UNIQUE,
    account_id    VARCHAR(50),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_permissions (
    user_id       BIGINT NOT NULL REFERENCES users(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (user_id, permission_id)
);

CREATE TABLE resources (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100),
    http_method   VARCHAR(10)  NOT NULL,
    path_pattern  VARCHAR(255) NOT NULL,
    permission_id BIGINT       NOT NULL REFERENCES permissions(id),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(255),
    CONSTRAINT uk_resources_method_path UNIQUE (http_method, path_pattern)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(255)
);

CREATE TABLE login_logs (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    username     VARCHAR(100) NOT NULL,
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(512),
    logged_in_at TIMESTAMP    NOT NULL
);

-- =============================================================================
-- SEED
-- =============================================================================

INSERT INTO accounts (id, status, deleted, created_at, updated_at) VALUES
    ('ACC-001', 'ACTIVE', false, NOW(), NOW()),
    ('ACC-002', 'ACTIVE', false, NOW(), NOW()),
    ('ACC-003', 'FROZEN', false, NOW(), NOW());

INSERT INTO account_balances (account_id, currency, available_amount, locked_amount, deleted, created_at, updated_at) VALUES
    ('ACC-001', 'VND', 10000000, 0, false, NOW(), NOW()),
    ('ACC-001', 'BTC',        5, 0, false, NOW(), NOW()),
    ('ACC-002', 'VND',  5000000, 0, false, NOW(), NOW()),
    ('ACC-002', 'BTC',        5, 0, false, NOW(), NOW()),
    ('ACC-003', 'VND',  2000000, 0, false, NOW(), NOW());

INSERT INTO order_books (id, base_currency, quote_currency, deleted, created_at, updated_at) VALUES
    ('BTC/VND', 'BTC', 'VND', false, NOW(), NOW());

-- orders / trades: để trống — test place + settle từ API

INSERT INTO permissions (name, description, deleted, created_at, updated_at) VALUES
    ('ORDER_PLACE',      'Đặt lệnh mua/bán',  false, NOW(), NOW()),
    ('ORDER_CANCEL',     'Huỷ lệnh',           false, NOW(), NOW()),
    ('ORDER_READ',       'Xem lệnh / sổ lệnh', false, NOW(), NOW()),
    ('ORDER_BOOK_OPEN',  'Mở sổ lệnh mới',     false, NOW(), NOW()),
    ('ACCOUNT_READ',     'Xem tài khoản',      false, NOW(), NOW()),
    ('ACCOUNT_DEPOSIT',  'Nạp tiền',           false, NOW(), NOW()),
    ('ACCOUNT_WITHDRAW', 'Rút tiền',           false, NOW(), NOW());

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_PLACE_API', 'POST', '/api/orders', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_PLACE';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_CANCEL_API', 'DELETE', '/api/orders/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_CANCEL';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_LIST_API', 'GET', '/api/orders/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_READ';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_BOOK_LIST_API', 'GET', '/api/order-books/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_READ';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_BOOK_OPEN_API', 'POST', '/api/order-books/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_BOOK_OPEN';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ACCOUNT_READ_API', 'GET', '/api/accounts/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ACCOUNT_READ';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ACCOUNT_DEPOSIT_API', 'POST', '/api/accounts/*/deposit', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ACCOUNT_DEPOSIT';

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ACCOUNT_WITHDRAW_API', 'POST', '/api/accounts/*/withdraw', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ACCOUNT_WITHDRAW';

INSERT INTO roles (name, description, deleted, created_at, updated_at) VALUES
    ('ROLE_ADMIN',    'Quản trị viên – toàn quyền',          false, NOW(), NOW()),
    ('ROLE_USER',     'Trader – đặt và huỷ lệnh',            false, NOW(), NOW()),
    ('ROLE_READONLY', 'Chỉ xem – không thực hiện giao dịch', false, NOW(), NOW());

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN (
    'ORDER_PLACE', 'ORDER_CANCEL', 'ORDER_READ', 'ACCOUNT_READ'
)
WHERE r.name = 'ROLE_USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN ('ORDER_READ', 'ACCOUNT_READ')
WHERE r.name = 'ROLE_READONLY';

-- password = password123 (BCrypt)
INSERT INTO users (username, password_hash, email, account_id, enabled, deleted, created_at, updated_at) VALUES
    ('admin',     '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'admin@example.com',    NULL,      true, false, NOW(), NOW()),
    ('trader1',   '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'trader1@example.com',  'ACC-001', true, false, NOW(), NOW()),
    ('trader2',   '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'trader2@example.com',  'ACC-002', true, false, NOW(), NOW()),
    ('readonly1', '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'readonly@example.com', NULL,      true, false, NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_ADMIN' WHERE u.username = 'admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_USER' WHERE u.username = 'trader1';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_USER' WHERE u.username = 'trader2';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_READONLY' WHERE u.username = 'readonly1';

COMMIT;

-- Kiểm tra nhanh
SELECT 'accounts' AS t, COUNT(*) FROM accounts
UNION ALL SELECT 'account_balances', COUNT(*) FROM account_balances
UNION ALL SELECT 'order_books', COUNT(*) FROM order_books
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'resources', COUNT(*) FROM resources
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'users', COUNT(*) FROM users;
