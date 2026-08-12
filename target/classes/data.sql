-- Seed data — Spring Boot tự load khi chạy với profile dev
-- (spring.jpa.defer-datasource-initialization=true)

INSERT INTO accounts (id, balance_amount, balance_currency, status, deleted, created_at, updated_at)
VALUES
    ('ACC-001', 10000000, 'VND', 'ACTIVE', false, NOW(), NOW()),
    ('ACC-002',  5000000, 'VND', 'ACTIVE', false, NOW(), NOW()),
    ('ACC-003',  2000000, 'VND', 'FROZEN', false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_books (id, base_currency, quote_currency, deleted, created_at, updated_at)
VALUES ('BTC/VND', 'BTC', 'VND', false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (
    id, account_id, side, order_type, base_currency, quote_currency,
    quantity, price, filled_quantity, status, deleted, created_at, updated_at
)
VALUES
    ('ORD-BUY-001', 'ACC-001', 'BUY', 'LIMIT', 'BTC', 'VND', 100, 60000000,  0, 'PENDING',           false, NOW(), NOW()),
    ('ORD-BUY-002', 'ACC-001', 'BUY', 'LIMIT', 'BTC', 'VND',  50, 59500000,  0, 'PENDING',           false, NOW(), NOW()),
    ('ORD-BUY-003', 'ACC-002', 'BUY', 'LIMIT', 'BTC', 'VND',  30, 58000000, 10, 'PARTIALLY_FILLED', false, NOW(), NOW()),
    ('ORD-SELL-001', 'ACC-002', 'SELL', 'LIMIT', 'BTC', 'VND',  50, 61000000,  0, 'PENDING', false, NOW(), NOW()),
    ('ORD-SELL-002', 'ACC-002', 'SELL', 'LIMIT', 'BTC', 'VND',  80, 62000000,  0, 'PENDING', false, NOW(), NOW()),
    ('ORD-SELL-003', 'ACC-001', 'SELL', 'LIMIT', 'BTC', 'VND',  20, 63500000, 20, 'FILLED',  false, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- RBAC seed data
-- ─────────────────────────────────────────────────────────────────────────────

-- Permissions (http_method + path_pattern = mapping API, không cần bảng api_permissions)
INSERT INTO permissions (name, description, http_method, path_pattern, deleted, created_at, updated_at)
VALUES
    ('ORDER_PLACE',      'Đặt lệnh mua/bán',     'POST',   '/api/orders',               false, NOW(), NOW()),
    ('ORDER_CANCEL',     'Huỷ lệnh',              'DELETE', '/api/orders/**',            false, NOW(), NOW()),
    ('ORDER_READ',       'Xem lệnh',              'GET',    '/api/orders/**',            false, NOW(), NOW()),
    ('ORDER_BOOK_READ',  'Xem order book',        'GET',    '/api/order-books/**',       false, NOW(), NOW()),
    ('ORDER_BOOK_OPEN',  'Mở sổ lệnh mới',        'POST',   '/api/order-books/**',       false, NOW(), NOW()),
    ('ACCOUNT_READ',     'Xem tài khoản',         'GET',    '/api/accounts/**',          false, NOW(), NOW()),
    ('ACCOUNT_DEPOSIT',  'Nạp tiền',              'POST',   '/api/accounts/*/deposit',   false, NOW(), NOW()),
    ('ACCOUNT_WITHDRAW', 'Rút tiền',              'POST',   '/api/accounts/*/withdraw',  false, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Cập nhật mapping nếu permission đã tồn tại từ lần seed trước
UPDATE permissions SET http_method = 'POST',   path_pattern = '/api/orders'              WHERE name = 'ORDER_PLACE';
UPDATE permissions SET http_method = 'DELETE', path_pattern = '/api/orders/**'           WHERE name = 'ORDER_CANCEL';
UPDATE permissions SET http_method = 'GET',    path_pattern = '/api/orders/**'           WHERE name = 'ORDER_READ';
UPDATE permissions SET http_method = 'GET',    path_pattern = '/api/order-books/**'      WHERE name = 'ORDER_BOOK_READ';
UPDATE permissions SET http_method = 'POST',   path_pattern = '/api/order-books/**'      WHERE name = 'ORDER_BOOK_OPEN';
UPDATE permissions SET http_method = 'GET',    path_pattern = '/api/accounts/**'         WHERE name = 'ACCOUNT_READ';
UPDATE permissions SET http_method = 'POST',   path_pattern = '/api/accounts/*/deposit'  WHERE name = 'ACCOUNT_DEPOSIT';
UPDATE permissions SET http_method = 'POST',   path_pattern = '/api/accounts/*/withdraw' WHERE name = 'ACCOUNT_WITHDRAW';

-- Gán ORDER_BOOK_READ cho các role (kể cả ADMIN nếu permission mới thêm sau)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name = 'ORDER_BOOK_READ'
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_USER', 'ROLE_READONLY')
ON CONFLICT DO NOTHING;

-- Roles
INSERT INTO roles (name, description, deleted, created_at, updated_at)
VALUES
    ('ROLE_ADMIN',    'Quản trị viên – toàn quyền',          false, NOW(), NOW()),
    ('ROLE_USER',     'Trader – đặt và huỷ lệnh',            false, NOW(), NOW()),
    ('ROLE_READONLY', 'Chỉ xem – không thực hiện giao dịch', false, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- role_permissions: ROLE_ADMIN có toàn bộ quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- role_permissions: ROLE_USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN (
    'ORDER_PLACE', 'ORDER_CANCEL', 'ORDER_READ', 'ORDER_BOOK_READ', 'ACCOUNT_READ'
)
WHERE r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- role_permissions: ROLE_READONLY
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name IN ('ORDER_READ', 'ORDER_BOOK_READ', 'ACCOUNT_READ')
WHERE r.name = 'ROLE_READONLY'
ON CONFLICT DO NOTHING;

-- Users  (mật khẩu BCrypt của chuỗi "password123")
INSERT INTO users (username, password_hash, email, account_id, enabled, deleted, created_at, updated_at)
VALUES
    ('admin',     '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'admin@example.com',    NULL,      true, false, NOW(), NOW()),
    ('trader1',   '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'trader1@example.com',  'ACC-001', true, false, NOW(), NOW()),
    ('readonly1', '$2y$12$2g6cmu2B2XHtRDC9Dr5G2.FwfycwW/cVP3sYnvFmensKsnggOBjeq', 'readonly@example.com', NULL,      true, false, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- user_roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username = 'trader1'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_READONLY'
WHERE u.username = 'readonly1'
ON CONFLICT DO NOTHING;
