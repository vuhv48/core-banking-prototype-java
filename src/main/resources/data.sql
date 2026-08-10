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
