# TAI-LIEU — Bank + Exchange (DDD / Hexagonal)

*File tài liệu duy nhất của project. Gồm: kiến trúc, domain, login/RBAC/resources, ErrorStatus, curl test, migration.*

---

## Mục lục

0. [Chuẩn bị & login](#0-chuẩn-bị--login)
1. [Dự án là gì](#1-dự-án-là-gì)
2. [Kiến trúc](#2-kiến-trúc)
3. [Cấu trúc thư mục](#3-cấu-trúc-thư-mục)
4. [Account](#4-account-bank)
5. [Exchange & khớp lệnh](#5-exchange--khớp-lệnh)
6. [Luồng Place Order](#6-luồng-place-order)
7. [Login & RBAC & resources](#7-login--rbac--resources)
8. [ErrorStatus](#8-errorstatus--apiresponse)
9. [API + curl](#9-api--curl)
10. [Kịch bản test đầy đủ](#10-kịch-bản-test-đầy-đủ)
11. [Phân loại DDD](#11-phân-loại-ddd)
12. [Lộ trình](#12-lộ-trình)
13. [File code tra cứu](#13-file-code-tra-cứu)

---

## 0. Chuẩn bị & login

### Chạy app

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Cần PostgreSQL, database `account_demo`. Profile `dev` load `data.sql`.

```bash
export BASE=http://localhost:8080
```

### User mẫu

| Username | Password | Role | Dùng để |
|----------|----------|------|---------|
| `admin` | `password123` | ROLE_ADMIN | Nạp/rút, toàn quyền |
| `trader1` | `password123` | ROLE_USER | Đặt lệnh |
| `readonly1` | `password123` | ROLE_READONLY | Test 403 |

### Lấy token

```bash
export TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

export TOKEN_ADMIN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

export TOKEN_RO=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"readonly1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
```

Response login:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "accessExpiresInSeconds": 900,
  "refreshExpiresInSeconds": 604800,
  "permissions": ["ORDER_PLACE", ...]
}
```

### Migration bảng `resources` (DB cũ)

```bash
psql -U postgres -d account_demo -f scripts/migrate-resources.sql
```

Hoặc mở `scripts/migrate-resources.sql` trong DBeaver → Execute.

Kiểm tra:

```sql
SELECT r.http_method, r.path_pattern, p.name AS permission
FROM resources r
JOIN permissions p ON p.id = r.permission_id
ORDER BY p.name, r.path_pattern;
```

Phải có **8 dòng**; `ORDER_READ` xuất hiện **2 lần**. Restart app → log: `Loaded 8 API resource rules from DB`.

---

## 1. Dự án là gì?

| Phần | Mô tả |
|------|--------|
| **Bank** | Account VND, nạp/rút |
| **Exchange** | Đặt lệnh BTC/VND, khớp LIMIT/MARKET |
| **Security** | JWT, RBAC + `resources`, login_logs, ErrorStatus |

Kiến trúc: **DDD** + **Hexagonal**. Domain không import Spring Security.

---

## 2. Kiến trúc

```
Request
  → JwtAuthFilter          (validate access JWT)
  → AuthorizationFilter    (match bảng resources → permission)
  → Controller (api/)
  → Application Service
  → Domain (pure Java)
  → Infrastructure (JPA / JWT)
  → PostgreSQL
```

Lỗi: `RestExceptionHandler` / `JsonErrorWriter` → `ApiResponse` + `ErrorStatus`.

---

## 3. Cấu trúc thư mục

```
src/main/java/com/example/accountdemo/
├── api/
│   ├── AccountController, OrderController
│   ├── auth/          # login, refresh, logout
│   ├── common/        # ErrorStatus, ApiError, ApiResponse, DomainException
│   └── error/         # RestExceptionHandler
├── application/
├── domain/
│   ├── account/
│   └── exchange/
└── infrastructure/
    ├── persistence/
    │   ├── account/, exchange/
    │   └── security/  # users, roles, permissions, resources, refresh_tokens, login_logs
    ├── security/      # JWT, filters, SecurityConfig
    ├── config/
    └── event/
```

---

## 4. Account (Bank)

| API | Permission |
|-----|------------|
| `POST /api/accounts/{id}/deposit` | ACCOUNT_DEPOSIT |
| `POST /api/accounts/{id}/withdraw` | ACCOUNT_WITHDRAW |

```bash
curl -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 100000, "currency": "VND"}'

curl -X POST $BASE/api/accounts/ACC-001/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount": 50000, "currency": "VND"}'

# Trader → 403
curl -i -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 100000, "currency": "VND"}'
```

> Exchange chưa trừ số dư Account khi đặt lệnh.

---

## 5. Exchange & khớp lệnh

### Khái niệm DDD

| Class | Loại |
|-------|------|
| `Order`, `OrderBook` | Aggregate Root |
| `Price`, `Quantity`, `Trade` | Value Object |
| `OrderMatchingService` | Domain Service (khớp trên RAM) |
| `TradeExecutedEvent` | Domain Event |

### Quy tắc khớp

| | Hành vi |
|--|---------|
| BUY | Tìm SELL giá thấp nhất |
| SELL | Tìm BUY giá cao nhất |
| LIMIT | Khớp nếu buyPrice ≥ sellPrice; dư treo sổ |
| MARKET | Khớp giá tốt nhất; dư bị cancel |

### Scenario nghiệp vụ (có Bearer)

**1) Bán 5 @ 60M → PENDING (hoặc FILLED nếu đã có mua)**

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'
```

**2) Mua 10 @ 60M → khớp 5, PARTIALLY_FILLED + log Trade executed**

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":60000000}'
```

**3) Mua @ 50M → không khớp, PENDING**

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":50000000}'
```

**4) Bán 5 @ 60M → khớp hết phần mua còn lại**

```bash
curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'
```

**5) Cặp ETH/VND chưa mở → ORDER_BOOK_NOT_OPEN**

```bash
curl -i -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"ETH","quoteCurrency":"VND","quantity":1,"price":1000000}'
```

**6) Readonly đặt lệnh → FORBIDDEN**

```bash
curl -i -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_RO" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'
```

```sql
SELECT id, side, price, quantity, filled_quantity, status
FROM orders ORDER BY created_at DESC LIMIT 10;
```

```bash
mvn test -Dtest=OrderMatchingServiceTest
```

---

## 6. Luồng Place Order

```
POST /api/orders + Bearer accessToken
  → JwtAuthFilter (JWT OK?)
  → AuthorizationFilter (có ORDER_PLACE? — đọc bảng resources)
  → OrderController
  → PlaceOrderApplicationService
      1. LOAD DB → OrderBook (list RAM)
      2. DOMAIN  → orderMatchingService.match(...)
      3. SAVE DB → orders + order_books
      4. EVENT   → TradeExecutedEvent (log)
```

`addOrder()` chỉ sửa list Java; persist ở bước 3. Lệnh FILLED remove khỏi sổ nhưng vẫn còn trong bảng `orders`.

---

## 7. Login & RBAC & resources

### Access vs Refresh

| | Access token | Refresh token |
|--|--------------|---------------|
| Là gì | JWT | Chuỗi ngẫu nhiên |
| Sống | 15 phút | 7 ngày |
| Lưu DB? | Không | Có — `token_hash` (SHA-256) |
| Dùng khi | `Authorization: Bearer ...` | `POST /api/auth/refresh` |

**`token_hash` = hash của refresh token, không phải access token.**

### Bảng

```
users
  ├── user_roles ──> roles ──> role_permissions ──> permissions
  └── user_permissions ──────────────────────────> permissions
                                                         ▲
resources (http_method + path_pattern) ──────────────────┘

refresh_tokens
login_logs
```

| Bảng | Vai trò |
|------|---------|
| `permissions` | Quyền nghiệp vụ — **không** chứa path |
| `resources` | Map API → permission (**1 permission nhiều path**) |

`ORDER_READ` gắn 2 resource:

| method | path | permission |
|--------|------|------------|
| GET | `/api/orders/**` | ORDER_READ |
| GET | `/api/order-books/**` | ORDER_READ |

### Filter (không @PreAuthorize)

```
JwtAuthFilter → AuthorizationFilter (resources) → Controller
```

Public paths (`application.yml`):

```yaml
security:
  public-paths:
    - /api/auth/login
    - /api/auth/refresh
```

### Auth curl

```bash
# Login
curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'

# Refresh
export REFRESH=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")

curl -s -X POST $BASE/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# Logout
curl -s -X POST $BASE/api/auth/logout -H "Authorization: Bearer $TOKEN"
```

```sql
SELECT r.http_method, r.path_pattern, p.name AS permission
FROM resources r JOIN permissions p ON p.id = r.permission_id
WHERE r.enabled = true ORDER BY p.name;

SELECT username, ip_address, logged_in_at FROM login_logs
ORDER BY logged_in_at DESC LIMIT 10;
```

---

## 8. ErrorStatus / ApiResponse

Pattern sale-app: `ErrorStatus` + `DomainException` + `RestExceptionHandler` + `JsonErrorWriter`.

```json
{
  "success": false,
  "data": null,
  "error": {
    "timestamp": "...",
    "status": 401,
    "code": "AUTH_FAILED",
    "message": "Đăng nhập thất bại",
    "path": "/api/auth/login",
    "details": {}
  }
}
```

| Code | HTTP | Khi nào |
|------|------|---------|
| AUTH_FAILED | 401 | Sai password |
| UNAUTHORIZED | 401 | Không token |
| REFRESH_TOKEN_INVALID | 401 | Refresh sai |
| FORBIDDEN | 403 | Thiếu quyền |
| ORDER_BOOK_NOT_OPEN | 400 | Cặp chưa mở |

```bash
curl -i -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"sai-mat-khau"}'

curl -i -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'

curl -i -X POST $BASE/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"token-gia"}'
```

---

## 9. API + curl

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/login` | public |
| POST | `/api/auth/refresh` | public |
| POST | `/api/auth/logout` | JWT |
| POST | `/api/orders` | ORDER_PLACE |
| POST | `/api/accounts/*/deposit` | ACCOUNT_DEPOSIT |
| POST | `/api/accounts/*/withdraw` | ACCOUNT_WITHDRAW |

Đã map trong `resources` nhưng chưa có controller: GET/DELETE orders, GET/POST order-books, GET accounts.

---

## 10. Kịch bản test đầy đủ

```bash
export BASE=http://localhost:8080

export TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

export TOKEN_ADMIN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -i -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"sai"}'

curl -X POST $BASE/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -d '{"amount":100000,"currency":"VND"}'

curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'

curl -X POST $BASE/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":60000000}'

curl -X POST $BASE/api/auth/logout -H "Authorization: Bearer $TOKEN"
```

---

## 11. Phân loại DDD

| Class | Loại |
|-------|------|
| Account, Order, OrderBook | Aggregate Root |
| Money, Price, Quantity, Trade | Value Object |
| OrderMatchingService | Domain Service |
| MatchResult | Result object |
| TradeExecutedEvent | Domain Event |
| PlaceOrderApplicationService | Application Service |
| JwtAuthFilter, resources, ErrorStatus | Infrastructure / API |

---

## 12. Lộ trình

### Đã xong

- [x] Account deposit/withdraw
- [x] Exchange domain + matching + TradeExecutedEvent
- [x] Login JWT + refresh (hash) + login_logs
- [x] RBAC + bảng `resources` (1 permission nhiều path)
- [x] Filter authz (không @PreAuthorize)
- [x] ErrorStatus / ApiResponse

### Chưa làm

- [ ] GET/DELETE orders, cancel
- [ ] Wallet / settlement
- [ ] Bảng `trades`
- [ ] Register / change-password
- [ ] Hot reload resources (không restart)

---

## 13. File code tra cứu

| File | Vai trò |
|------|---------|
| `SecurityConfig.java` | Filter chain, public paths, 401/403 JSON |
| `JwtAuthFilter.java` | Validate access JWT |
| `AuthorizationFilter.java` | Check permission qua `resources` |
| `ApiPermissionRuleRegistry.java` | Cache rules từ `resources` |
| `ResourceJpaEntity.java` | Bảng resources |
| `JwtUtil.java` | JWT + hash refresh |
| `AuthController.java` | login / refresh / logout |
| `ErrorStatus.java` | Mã lỗi |
| `RestExceptionHandler.java` | Map exception → JSON |
| `scripts/migrate-resources.sql` | Migration DB → resources |
| **`TAI-LIEU.md`** | **File tài liệu duy nhất** |
