# Tài liệu tổng hợp — Dự án Bank + Exchange (DDD / Hexagonal)

*Tài liệu này giải thích toàn bộ project từ Sprint 0 (Account) đến Sprint 5 (Domain Event) và phần Login/RBAC. Mỗi phần có kèm lệnh `curl` để test.*

---

## 0. Chuẩn bị trước khi test

### Chạy app

```bash
# Cần PostgreSQL, database account_demo
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Profile `dev` load seed từ `data.sql`.

### Biến môi trường (dùng xuyên suốt các lệnh curl bên dưới)

```bash
export BASE=http://localhost:8080
```

### Login lấy token (chạy 1 lần đầu phiên test)

**Trader** — đặt lệnh, xem (không nạp/rút):

```bash
export TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo $TOKEN
```

**Admin** — toàn quyền (nạp/rút tiền):

```bash
export TOKEN_ADMIN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
```

**Readonly** — chỉ xem, test 403:

```bash
export TOKEN_RO=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"readonly1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
```

> Không có `python3`/`jq`: copy `accessToken` từ response JSON, rồi `export TOKEN="eyJ..."`.

### User mẫu

| Username | Password | Role | Dùng để test |
|----------|----------|------|--------------|
| `admin` | `password123` | ROLE_ADMIN | Nạp/rút tiền |
| `trader1` | `password123` | ROLE_USER | Đặt lệnh |
| `readonly1` | `password123` | ROLE_READONLY | Test 403 |

---

## 1. Dự án này là gì?

Project **accountdemo** mô phỏng một hệ thống **ngân hàng + sàn giao dịch (exchange)** đơn giản:

| Phần | Mô tả |
|------|--------|
| **Bank (Account)** | Tài khoản VND, nạp/rút tiền |
| **Exchange** | Đặt lệnh mua/bán BTC/VND, khớp lệnh LIMIT/MARKET |
| **Security** | Login JWT, RBAC, audit log đăng nhập |

Kiến trúc: **DDD (Domain-Driven Design)** + **Hexagonal (Ports & Adapters)**.

**Test nhanh toàn hệ thống (3 lệnh):**

```bash
# 1) Login
curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'

# 2) Đặt lệnh (thay <TOKEN> bằng accessToken vừa nhận)
curl -s -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'

# 3) Xem lệnh trong DB
# psql -c "SELECT id, side, price, status FROM orders ORDER BY created_at DESC LIMIT 5;"
```

---

## 2. Sơ đồ kiến trúc tổng quan

```
                    ┌─────────────────────────────────────┐
                    │              api/                    │
                    │  AccountController, OrderController  │
                    │  AuthController                      │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │          application/                │
                    │  DepositApplicationService           │
                    │  WithdrawApplicationService          │
                    │  PlaceOrderApplicationService        │
                    └──────────────┬──────────────────────┘
                                   │ gọi interface (port)
          ┌────────────────────────┼────────────────────────┐
          │                        │                        │
┌─────────▼─────────┐   ┌─────────▼─────────┐   ┌─────────▼─────────┐
│   domain/account  │   │  domain/exchange  │   │ infrastructure/   │
│   Account, Money  │   │ Order, OrderBook  │   │ JPA, Security, JWT│
│   (pure Java)     │   │ OrderMatchingSvc  │   │ (adapter)         │
└───────────────────┘   └───────────────────┘   └───────────────────┘
                                                        │
                                                        ▼
                                                   PostgreSQL
```

**Nguyên tắc:** Domain **không** import Spring Security.

**curl minh hoạ luồng qua các layer** (1 request đặt lệnh):

```bash
curl -v -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'
```

Thứ tự xử lý: `api/` → filter security → `application/` → `domain/` → save DB.

---

## 3. Cấu trúc thư mục

*(Không có API riêng — xem mục 4–8 để test từng layer qua curl.)*

---

## 4. Domain — Account (Sprint 0)

### Aggregate Root: `Account`

- `accountId` (String, vd `ACC-001`)
- `balance` (`Money` — amount + currency)
- `status` (`ACTIVE`, `FROZEN`)

### Use case

| API | Application Service | Domain |
|-----|---------------------|--------|
| `POST /api/accounts/{id}/deposit` | `DepositApplicationService` | `Account.deposit()` |
| `POST /api/accounts/{id}/withdraw` | `WithdrawApplicationService` | `Account.withdraw()` |

**Lưu ý:** Cần quyền `ACCOUNT_DEPOSIT` / `ACCOUNT_WITHDRAW` → dùng user **admin**.

### curl — Nạp tiền ACC-001

```bash
curl -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 100000, "currency": "VND"}'
```

→ HTTP 200, số dư tăng. Không tạo lệnh exchange.

### curl — Rút tiền ACC-001

```bash
curl -X POST $BASE/api/accounts/ACC-001/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 50000, "currency": "VND"}'
```

→ HTTP 200 nếu đủ số dư và account không `FROZEN`.

### curl — Test 403: trader1 không được nạp tiền

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 100000, "currency": "VND"}'
```

→ Kỳ vọng **403** (trader1 không có `ACCOUNT_DEPOSIT`).

### curl — Test 401: không gửi token

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 100000, "currency": "VND"}'
```

→ Kỳ vọng **401** hoặc **403** (chưa xác thực).

---

## 5. Domain — Exchange (Sprint 1–5)

### Quy tắc khớp lệnh

| Loại | Hành vi |
|------|---------|
| **BUY** | Tìm lệnh **SELL** giá **thấp nhất** trên sổ |
| **SELL** | Tìm lệnh **BUY** giá **cao nhất** trên sổ |
| **LIMIT** | Chỉ khớp nếu `buyPrice >= sellPrice`; phần dư treo sổ |
| **MARKET** | Khớp giá tốt nhất; phần dư **bị cancel** |

Seed `dev` thường có sẵn lệnh trên sổ BTC/VND, bán thấp nhất khoảng **61.000.000**.

### curl — 5.1) Đặt lệnh BÁN — tạo thanh khoản trên sổ

Bán 5 BTC @ 60.000.000 (ACC-002):

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "accountId": "ACC-002",
    "side": "SELL",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 5,
    "price": 60000000
  }'
```

→ Nếu chưa có mua @ ≥ 60M: `"status":"PENDING"`.

### curl — 5.2) Đặt lệnh MUA — khớp một phần

Mua 10 @ 60M (sau khi đã có bán 5 @ 60M ở bước 5.1):

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "accountId": "ACC-001",
    "side": "BUY",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 10,
    "price": 60000000
  }'
```

→ Khớp 5: bán **FILLED**, mua **PARTIALLY_FILLED**. Console log: `Trade executed`.

### curl — 5.3) Không khớp — giá mua thấp hơn giá bán

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "accountId": "ACC-001",
    "side": "BUY",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 10,
    "price": 50000000
  }'
```

→ `"status":"PENDING"`, không có log Trade executed.

### curl — 5.4) Khớp hết — bán đúng khối lượng còn lại

Sau bước 5.2 còn 5 mua chờ @ 60M, đặt bán 5 @ 60M:

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "accountId": "ACC-002",
    "side": "SELL",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 5,
    "price": 60000000
  }'
```

→ Cả hai lệnh **FILLED**.

### curl — 5.5) Cặp chưa mở — bị reject (domain)

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "accountId": "ACC-001",
    "side": "BUY",
    "orderType": "LIMIT",
    "baseCurrency": "ETH",
    "quoteCurrency": "VND",
    "quantity": 1,
    "price": 1000000
  }'
```

→ Lỗi *"Cặp giao dịch chưa được mở"* (HTTP 500 hoặc 400 tùy exception handler).

### curl — 5.6) Test 403: readonly không được đặt lệnh

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_RO" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'
```

→ Kỳ vọng **403** (thiếu `ORDER_PLACE`).

---

## 6. Luồng đặt lệnh (Place Order)

```
POST /api/orders  (+ JWT)
  → JwtAuthFilter + AuthorizationFilter
  → OrderController
  → PlaceOrderApplicationService
      1. LOAD DB  (orderBookRepository.findByTradingPair)
      2. DOMAIN   (orderMatchingService.match — chỉ RAM)
      3. SAVE DB  (orderRepository.save + orderBookRepository.save)
      4. EVENT    (TradeExecutedEvent → log console)
```

**curl + kiểm tra DB ngay sau:**

```bash
# Đặt lệnh
curl -s -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":61000000}'

# Xem kết quả trong PostgreSQL
psql account_demo -c "
SELECT id, side, price, quantity, filled_quantity, status
FROM orders ORDER BY created_at DESC LIMIT 5;"
```

**Gợi ý thứ tự test hiểu nghiệp vụ:** 5.1 → 5.2 → 5.3 → 5.4 (mục trên).

---

## 7. Hệ thống Login & RBAC

### 7.1 Tại sao Auth nằm ở Infrastructure?

Domain `Order`, `Account` **không biết** user đang login là ai.

### 7.2 Các bảng database

8 bảng: `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `user_permissions`, `refresh_tokens`, `login_logs`.

Bảng `permissions` có thêm `http_method` + `path_pattern` để map API (không cần bảng `api_permissions` riêng).

### 7.3 curl — Login (public, không cần token)

```bash
curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'
```

Response mẫu:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "a1b2c3d4...",
  "permissions": ["ORDER_PLACE", "ORDER_CANCEL", "ORDER_READ", "ORDER_BOOK_READ", "ACCOUNT_READ"]
}
```

### curl — Login sai password → 401

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"sai-mat-khau"}'
```

### curl — Login bằng GET trên browser → 403

```bash
curl -w "\nHTTP:%{http_code}\n" -X GET $BASE/api/auth/login
```

→ Login chỉ nhận **POST** + JSON body.

### 7.4 curl — Refresh token

```bash
# Lấy refresh token từ login
export REFRESH=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")

curl -s -X POST $BASE/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

→ Trả `accessToken` + `refreshToken` mới; token cũ bị revoke.

### curl — Refresh token không hợp lệ → 401

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"token-gia-ma"}'
```

### 7.5 curl — Logout

```bash
curl -s -X POST $BASE/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

Response: `{"message":"Đăng xuất thành công"}`

### 7.6 Luồng Filter (không dùng @PreAuthorize)

```
JwtAuthFilter       → đọc JWT, set SecurityContext
AuthorizationFilter → đọc permissions table, kiểm tra quyền
Controller
```

**3 lớp config:**

| Nguồn | Config |
|-------|--------|
| `application.yml` → `security.public-paths` | login, refresh |
| Bảng `permissions` | API → permission |
| Bảng `roles` + join | user → quyền |

### curl — Test 401: gọi API không có token

```bash
curl -w "\nHTTP:%{http_code}\n" -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'
```

### curl — Test 403: readonly đặt lệnh

*(Xem mục 5.6)*

### 7.7 Cấu hình yaml

```yaml
security:
  jwt:
    secret: "change-me-to-a-very-long-secret-key-32chars!"
    access-token-ms: 900000
    refresh-token-ms: 604800000
  public-paths:
    - /api/auth/login
    - /api/auth/refresh
```

### 7.8 curl — Kiểm tra login_logs sau khi login

```bash
# Login (tạo 1 dòng log)
curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' > /dev/null

# Xem DB
psql account_demo -c "
SELECT id, username, ip_address, user_agent, logged_in_at
FROM login_logs ORDER BY logged_in_at DESC LIMIT 5;"
```

### 7.9 Mã lỗi HTTP

| Mã | Ý nghĩa | curl test |
|----|---------|-----------|
| **401** | Chưa login / sai password / token hết hạn | Mục 7.3, 7.4, không gửi Bearer |
| **403** | Đã login nhưng thiếu quyền | Mục 4, 5.6 (readonly/trader) |

---

## 8. Danh sách API + curl từng endpoint

### Public (không cần token)

**POST /api/auth/login**

```bash
curl -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'
```

**POST /api/auth/refresh**

```bash
curl -X POST $BASE/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

### Cần JWT (+ permission nếu có rule trong DB)

**POST /api/auth/logout** — chỉ cần authenticated

```bash
curl -X POST $BASE/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

**POST /api/orders** — `ORDER_PLACE`

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'
```

**POST /api/accounts/{id}/deposit** — `ACCOUNT_DEPOSIT` (admin)

```bash
curl -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 100000, "currency": "VND"}'
```

**POST /api/accounts/{id}/withdraw** — `ACCOUNT_WITHDRAW` (admin)

```bash
curl -X POST $BASE/api/accounts/ACC-001/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 50000, "currency": "VND"}'
```

**Các endpoint đã cấu hình permission nhưng chưa implement controller:**

| Method | Path | Permission | Ghi chú |
|--------|------|------------|---------|
| DELETE | `/api/orders/**` | ORDER_CANCEL | Chưa có API |
| GET | `/api/orders/**` | ORDER_READ | Chưa có API |
| GET | `/api/order-books/**` | ORDER_BOOK_READ | Chưa có API |
| POST | `/api/order-books/**` | ORDER_BOOK_OPEN | Chưa có API |
| GET | `/api/accounts/**` | ACCOUNT_READ | Chưa có API |

Test permission (sẽ 405 Not Found hoặc 404, không phải 403 nếu chưa có handler):

```bash
curl -w "\nHTTP:%{http_code}\n" -X GET $BASE/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

---

## 9. Kịch bản test đầy đủ (copy-paste tuần tự)

```bash
export BASE=http://localhost:8080

# ── Auth ──────────────────────────────────────────
curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'

export TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

export TOKEN_ADMIN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# ── Account (admin) ───────────────────────────────
curl -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 100000, "currency": "VND"}'

# ── Exchange: bán rồi mua khớp ────────────────────
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'

curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":60000000}'

# ── Logout ────────────────────────────────────────
curl -X POST $BASE/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

### Kiểm tra DB

```sql
-- Lệnh
SELECT id, side, price, quantity, filled_quantity, status
FROM orders ORDER BY created_at DESC LIMIT 10;

-- Login audit
SELECT username, ip_address, user_agent, logged_in_at
FROM login_logs ORDER BY logged_in_at DESC;

-- Quyền trader1
SELECT p.name, p.http_method, p.path_pattern
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN role_permissions rp ON ur.role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.username = 'trader1';
```

### Test domain không cần API

```bash
mvn test -Dtest=OrderMatchingServiceTest
```

---

## 10. Phân loại DDD — tóm tắt nhanh

| Class | Loại DDD |
|-------|----------|
| `Account`, `Order`, `OrderBook` | Aggregate Root |
| `Money`, `Price`, `Quantity`, `Trade` | Value Object |
| `OrderMatchingService` | Domain Service |
| `MatchResult` | Result object |
| `TradeExecutedEvent` | Domain Event |
| `PlaceOrderApplicationService` | Application Service |
| `UserJpaEntity`, `JwtAuthFilter` | Infrastructure |

---

## 11. Lộ trình đã làm & chưa làm

### Đã xong

- [x] Sprint 0–5: Account, Exchange, Matching, Event
- [x] Login JWT + RBAC + AuthorizationFilter + login_logs

### Chưa làm

- [ ] GET/DELETE API orders
- [ ] Wallet / settlement
- [ ] Bảng `trades`
- [ ] Hot reload permission rules

---

## 12. Tài liệu liên quan

| File | Nội dung |
|------|----------|
| `13-giai-doan-4-sprint-1-domain-exchange.md` | Domain Exchange |
| `15-giai-doan-4-sprint-3-4-5.md` | Sprint 3–5 |
| `scripts/api-test-nghiep-vu.md` | Test khớp lệnh (bản cũ, chưa có auth header) |
| **`16-tai-lieu-tong-hop-du-an-va-login.md`** | **Tài liệu này** |

---

## 13. File Security tra cứu nhanh

| File | Vai trò |
|------|---------|
| `SecurityConfig.java` | Filter chain, public paths yaml |
| `JwtAuthFilter.java` | Authentication (JWT) |
| `AuthorizationFilter.java` | Authorization (DB permissions) |
| `AuthController.java` | login / refresh / logout |
