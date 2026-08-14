-- Seed data — Spring Boot tự load khi chạy với profile dev
-- (spring.jpa.defer-datasource-initialization=true)

INSERT INTO accounts (id, status, deleted, created_at, updated_at)
VALUES
    ('ACC-001', 'ACTIVE', false, NOW(), NOW()),
    ('ACC-002', 'ACTIVE', false, NOW(), NOW()),
    ('ACC-003', 'FROZEN', false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Số dư theo currency (nguồn sự thật cho wallet)
INSERT INTO account_balances (account_id, currency, available_amount, locked_amount, deleted, created_at, updated_at)
VALUES
    ('ACC-001', 'VND', 10000000, 0, false, NOW(), NOW()),
    ('ACC-001', 'BTC',        5, 0, false, NOW(), NOW()),
    ('ACC-002', 'VND',  5000000, 0, false, NOW(), NOW()),
    ('ACC-002', 'BTC',        5, 0, false, NOW(), NOW()),
    ('ACC-003', 'VND',  2000000, 0, false, NOW(), NOW())
ON CONFLICT (account_id, currency) DO NOTHING;

INSERT INTO order_books (id, base_currency, quote_currency, deleted, created_at, updated_at)
VALUES ('BTC/VND', 'BTC', 'VND', false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Không seed lệnh PENDING (tránh lệch locked). Sổ trống để test place/settle.

-- ─────────────────────────────────────────────────────────────────────────────
-- RBAC seed data
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO permissions (name, description, deleted, created_at, updated_at)
VALUES
    ('ORDER_PLACE',      'Đặt lệnh mua/bán',  false, NOW(), NOW()),
    ('ORDER_CANCEL',     'Huỷ lệnh',           false, NOW(), NOW()),
    ('ORDER_READ',       'Xem lệnh / sổ lệnh', false, NOW(), NOW()),
    ('ORDER_BOOK_OPEN',  'Mở sổ lệnh mới',     false, NOW(), NOW()),
    ('ACCOUNT_READ',     'Xem tài khoản',      false, NOW(), NOW()),
    ('ACCOUNT_DEPOSIT',  'Nạp tiền',           false, NOW(), NOW()),
    ('ACCOUNT_WITHDRAW', 'Rút tiền',           false, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_PLACE_API', 'POST', '/api/orders', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_PLACE'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_CANCEL_API', 'DELETE', '/api/orders/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_CANCEL'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_LIST_API', 'GET', '/api/orders/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_READ'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_BOOK_LIST_API', 'GET', '/api/order-books/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_READ'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ORDER_BOOK_OPEN_API', 'POST', '/api/order-books/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ORDER_BOOK_OPEN'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ACCOUNT_READ_API', 'GET', '/api/accounts/**', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ACCOUNT_READ'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ACCOUNT_DEPOSIT_API', 'POST', '/api/accounts/*/deposit', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ACCOUNT_DEPOSIT'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
SELECT 'ACCOUNT_WITHDRAW_API', 'POST', '/api/accounts/*/withdraw', p.id, true, false, NOW(), NOW()
FROM permissions p WHERE p.name = 'ACCOUNT_WITHDRAW'
ON CONFLICT (http_method, path_pattern) DO NOTHING;

INSERT INTO roles (name, description, deleted, created_at, updated_at)
VALUES
    ('ROLE_ADMIN',    'Quản trị viên – toàn quyền',          false, NOW(), NOW()),
    ('ROLE_USER',     'Trader – đặt và huỷ lệnh',            false, NOW(), NOW()),
    ('ROLE_READONLY', 'Chỉ xem – không thực hiện giao dịch', false, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN (
    'ORDER_PLACE', 'ORDER_CANCEL', 'ORDER_READ', 'ACCOUNT_READ'
)
WHERE r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN ('ORDER_READ', 'ACCOUNT_READ')
WHERE r.name = 'ROLE_READONLY'
ON CONFLICT DO NOTHING;

INSERT INTO users (username, password_hash, email, account_id, enabled, deleted, created_at, updated_at)
VALUES
    ('admin',     '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'admin@example.com',    NULL,      true, false, NOW(), NOW()),
    ('trader1',   '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'trader1@example.com',  'ACC-001', true, false, NOW(), NOW()),
    ('trader2',   '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'trader2@example.com',  'ACC-002', true, false, NOW(), NOW()),
    ('readonly1', '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'readonly@example.com', NULL,      true, false, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username = 'trader1'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username = 'trader2'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_READONLY'
WHERE u.username = 'readonly1'
ON CONFLICT DO NOTHING;
