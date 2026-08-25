# Curl / Postman — Bank + Exchange API

**Environment Postman** (tạo trước khi import):

| Variable | Initial value |
|---|---|
| `BASE` | `http://localhost:8080` |
| `TOKEN` | *(để trống — login sẽ set)* |
| `REFRESH_TOKEN` | *(để trống)* |
| `ORDER_ID` | *(điền sau khi place order)* |

**Cách dùng:** Import → Raw text → dán từng lệnh curl. URL dùng `{{BASE}}/...`.

**Users seed** (`scripts/init-full.sql`):

| Username | Password | Account ID | Role |
|---|---|---|---|
| `admin` | `password123` | — | ROLE_ADMIN (toàn quyền) |
| `trader1` | `password123` | `ACC-001` | ROLE_USER |
| `trader2` | `password123` | `ACC-002` | ROLE_USER |
| `readonly1` | `password123` | — | ROLE_READONLY |

---

## 1. Auth

### 1.1 Đăng ký

```bash
curl -X POST '{{BASE}}/api/auth/register' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "trader3",
    "password": "password123",
    "email": "trader3@example.com"
  }'
```

### 1.2 Đăng nhập

```bash
curl -X POST '{{BASE}}/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "trader1",
    "password": "password123"
  }'
```

Login admin:

```bash
curl -X POST '{{BASE}}/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

### 1.3 Refresh token

```bash
curl -X POST '{{BASE}}/api/auth/refresh' \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "{{REFRESH_TOKEN}}"
  }'
```

### 1.4 Logout

```bash
curl -X POST '{{BASE}}/api/auth/logout' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

---

## 2. Account / Ví

### 2.0 List account có phân trang (admin only)

```bash
curl -X GET '{{BASE}}/api/accounts?page=0&size=20' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

Query params:
- `page` — trang **0-based** (mặc định `0`)
- `size` — số bản ghi / trang (mặc định `20`, tối đa `100`)

Response mẫu:

```json
{
  "content": [
    { "accountId": "ACC-001", "status": "ACTIVE", "balances": [] }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "hasNext": false
}
```

> Chỉ `ROLE_ADMIN`. Trader gọi sẽ 403. Login bằng `admin` trước.
>
> **DB đã seed trước đó:** chạy SQL rồi restart BE:
> ```sql
> INSERT INTO resources (name, http_method, path_pattern, permission_id, enabled, deleted, created_at, updated_at)
> SELECT 'ACCOUNT_LIST_API', 'GET', '/api/accounts', p.id, true, false, NOW(), NOW()
> FROM permissions p WHERE p.name = 'ACCOUNT_READ'
> ON CONFLICT DO NOTHING;
> ```
> (Nếu không có unique constraint: bỏ `ON CONFLICT`, kiểm tra trùng bằng `SELECT * FROM resources WHERE path_pattern = '/api/accounts'`.)

### 2.1 Xem số dư

```bash
curl -X GET '{{BASE}}/api/accounts/ACC-001' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

### 2.2 Tạo ví / account (admin) — không tạo login

```bash
curl -X POST '{{BASE}}/api/accounts' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "accountId": "ACC-003",
    "status": "ACTIVE",
    "holdings": [
      { "currency": "VND", "available": 100000000 },
      { "currency": "BTC", "available": 2 }
    ]
  }'
```

### 2.2b Admin tạo user (có login)

Tạo user + ví mới:

```bash
curl -X POST '{{BASE}}/api/admin/users' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "trader9",
    "password": "password123",
    "email": "trader9@example.com",
    "accountId": null
  }'
```

Gắn ví đã có (vd. ACC-003 vừa tạo):

```bash
curl -X POST '{{BASE}}/api/admin/users' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "trader9",
    "password": "password123",
    "email": null,
    "accountId": "ACC-003"
  }'
```

### 2.3 Nạp tiền / deposit

Trader nạp **ví của mình**; admin (không gắn account) nạp hộ được mọi ví.

> Amount / quantity / price dùng **số thập phân** được (`BigDecimal`, scale tối đa 8).

```bash
curl -X POST '{{BASE}}/api/accounts/ACC-001/deposit' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "amount": 10000000.5,
    "currency": "VND"
  }'
```

Nạp BTC thập phân:

```bash
curl -X POST '{{BASE}}/api/accounts/ACC-001/deposit' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "amount": 1.1,
    "currency": "BTC"
  }'
```

### 2.4 Rút tiền / withdraw

```bash
curl -X POST '{{BASE}}/api/accounts/ACC-001/withdraw' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "amount": 1000000,
    "currency": "VND"
  }'
```

### 2.5 Chuyển tiền / transfer

```bash
curl -X POST '{{BASE}}/api/accounts/transfer' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "fromAccountId": "ACC-001",
    "toAccountId": "ACC-002",
    "amount": 500000,
    "currency": "VND"
  }'
```

### 2.6 Freeze account (admin)

```bash
curl -X POST '{{BASE}}/api/accounts/ACC-001/freeze' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

### 2.7 Unfreeze account (admin)

```bash
curl -X POST '{{BASE}}/api/accounts/ACC-001/unfreeze' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

### 2.8 List orders theo account

```bash
curl -X GET '{{BASE}}/api/accounts/ACC-001/orders?page=0&size=10' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

### 2.8b Admin list / tìm lệnh (phân trang)

```bash
curl -X GET '{{BASE}}/api/orders?page=0&size=10' \
  -H 'Authorization: Bearer {{TOKEN}}'

curl -X GET '{{BASE}}/api/orders?accountId=ACC-001&page=0&size=10' \
  -H 'Authorization: Bearer {{TOKEN}}'

curl -X GET '{{BASE}}/api/orders?orderId={{ORDER_ID}}' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

---

## 3. Orders / Lệnh

### 3.1 Đặt lệnh BUY LIMIT

```bash
curl -X POST '{{BASE}}/api/orders' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "accountId": "ACC-001",
    "side": "BUY",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 1,
    "price": 60000000
  }'
```

### 3.2 Đặt lệnh SELL LIMIT

```bash
curl -X POST '{{BASE}}/api/orders' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "accountId": "ACC-002",
    "side": "SELL",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 1,
    "price": 60000000
  }'
```

### 3.3 Đặt lệnh SELL MARKET

```bash
curl -X POST '{{BASE}}/api/orders' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{
    "accountId": "ACC-002",
    "side": "SELL",
    "orderType": "MARKET",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 1,
    "price": null
  }'
```

### 3.4 Xem chi tiết lệnh

```bash
curl -X GET '{{BASE}}/api/orders/{{ORDER_ID}}' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

### 3.5 Hủy lệnh

```bash
curl -X DELETE '{{BASE}}/api/orders/{{ORDER_ID}}' \
  -H 'Authorization: Bearer {{TOKEN}}'
```

---

## 4. Kịch bản E2E nhanh (copy lần lượt)

```bash
# 1) Login admin → lấy TOKEN
curl -X POST '{{BASE}}/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"password123"}'

# 1b) List account trang 0, size 20
curl -X GET '{{BASE}}/api/accounts?page=0&size=20' \
  -H 'Authorization: Bearer {{TOKEN}}'

# 2) Deposit thêm VND cho ACC-001 (dùng TOKEN admin)
curl -X POST '{{BASE}}/api/accounts/ACC-001/deposit' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{"amount":50000000,"currency":"VND"}'

# 3) Login trader1 → TOKEN trader
curl -X POST '{{BASE}}/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"trader1","password":"password123"}'

# 4) Xem ví
curl -X GET '{{BASE}}/api/accounts/ACC-001' \
  -H 'Authorization: Bearer {{TOKEN}}'

# 5) Đặt BUY LIMIT
curl -X POST '{{BASE}}/api/orders' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'

# 6) Login trader2 → đặt SELL LIMIT (khớp)
curl -X POST '{{BASE}}/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"trader2","password":"password123"}'

curl -X POST '{{BASE}}/api/orders' \
  -H 'Authorization: Bearer {{TOKEN}}' \
  -H 'Content-Type: application/json' \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'

# 7) Kiểm tra số dư hai bên
curl -X GET '{{BASE}}/api/accounts/ACC-001' -H 'Authorization: Bearer {{TOKEN}}'
curl -X GET '{{BASE}}/api/accounts/ACC-002' -H 'Authorization: Bearer {{TOKEN}}'
```

---

## 5. Postman tip — auto set TOKEN sau login

1. Environment: `BASE` = `http://localhost:8080`, `TOKEN` / `REFRESH_TOKEN` để trống.
2. Request Login → tab **Tests**:

```javascript
const json = pm.response.json();
if (json.accessToken) {
  pm.environment.set('TOKEN', json.accessToken);
}
if (json.refreshToken) {
  pm.environment.set('REFRESH_TOKEN', json.refreshToken);
}
```

3. Place order → Tests (tuỳ response field `orderId`):

```javascript
const json = pm.response.json();
if (json.orderId) {
  pm.environment.set('ORDER_ID', json.orderId);
}
```

4. Các request khác dùng header: `Authorization: Bearer {{TOKEN}}`.

> Lỗi BE: `{ "success": false, "error": { "status", "code", "message" } }` — đọc `error.message`.
