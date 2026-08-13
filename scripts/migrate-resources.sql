-- Migration: tách mapping API sang bảng resources
-- Chạy trên database account_demo (DBeaver / psql) SAU KHI restart app 1 lần
-- (để Hibernate tạo bảng resources), HOẶC tạo bảng thủ công như bên dưới rồi seed.

-- 1) Tạo bảng resources (nếu app chưa tạo)
CREATE TABLE IF NOT EXISTS resources (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100),
    http_method   VARCHAR(10)  NOT NULL,
    path_pattern  VARCHAR(255) NOT NULL,
    permission_id BIGINT       NOT NULL REFERENCES permissions(id),
    enabled       BOOLEAN      NOT NULL DEFAULT true,
    deleted       BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(255),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(255),
    CONSTRAINT uk_resources_method_path UNIQUE (http_method, path_pattern)
);

-- 2) Seed resources (1 permission ORDER_READ → 2 path)
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

-- 3) Gán lại role (bỏ ORDER_BOOK_READ nếu còn; ORDER_READ đủ cho xem orders + order-books)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.name = 'ORDER_READ'
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_USER', 'ROLE_READONLY')
ON CONFLICT DO NOTHING;

-- 4) (Tuỳ chọn) dọn permission cũ ORDER_BOOK_READ
DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE name = 'ORDER_BOOK_READ');

DELETE FROM user_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE name = 'ORDER_BOOK_READ');

DELETE FROM permissions WHERE name = 'ORDER_BOOK_READ';

-- 5) (Tuỳ chọn) xoá cột cũ trên permissions nếu còn
ALTER TABLE permissions DROP COLUMN IF EXISTS http_method;
ALTER TABLE permissions DROP COLUMN IF EXISTS path_pattern;

-- 6) Kiểm tra
SELECT r.id, r.name, r.http_method, r.path_pattern, p.name AS permission, r.enabled
FROM resources r
JOIN permissions p ON p.id = r.permission_id
ORDER BY p.name, r.path_pattern;
