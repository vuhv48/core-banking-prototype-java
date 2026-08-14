# TAI-LIEU — Bank + Exchange (DDD / Hexagonal)

*File tài liệu duy nhất của project. Gồm: kiến trúc, domain, login/RBAC, ErrorStatus, curl, DB.*

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
14. [Wallet / Settlement / Cancel / Ownership](#14-hướng-dẫn-wallet--settlement--cancel--ownership)

---

## 0. Chuẩn bị & login

### DB + chạy app

```bash
# Tạo DB nếu chưa có
psql -U postgres -c "CREATE DATABASE account_demo;"

# Schema + seed (file SQL duy nhất trong scripts/)
psql -U postgres -d account_demo -f scripts/init-full.sql

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

DBeaver: connect `account_demo` → Execute `scripts/init-full.sql`.

Base URL: `http://localhost:8080`

Chạy app **sau** khi đã execute `scripts/init-full.sql`.

### User mẫu

| Username | Password | Role | Account | Dùng để |
|----------|----------|------|---------|---------|
| `admin` | `password123` | ROLE_ADMIN | — | Nạp/rút, toàn quyền |
| `trader1` | `password123` | ROLE_USER | ACC-001 | Đặt lệnh |
| `trader2` | `password123` | ROLE_USER | ACC-002 | Đặt lệnh đối ứng |
| `readonly1` | `password123` | ROLE_READONLY | — | Test 403 |

### Lấy token (Postman / curl)

**Trader** — đặt lệnh:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'
```

**Admin** — nạp/rút tiền:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```

**Readonly** — test 403:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"readonly1","password":"password123"}'
```

Response:

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

Trong Postman: copy `accessToken` → tab **Authorization** → Type **Bearer Token** → dán vào.  
Gọi API khác thêm header: `Authorization: Bearer <accessToken>`.

Kiểm tra RBAC sau seed:

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

Package domain theo **bounded context → feature/aggregate** (không tách `vo/` / `enum/` / `repo/`).

```
src/main/java/com/example/accountdemo/
├── api/
│   ├── AccountController, OrderController
│   ├── auth/          # login, refresh, logout
│   ├── common/        # ErrorStatus, ApiError, ApiResponse, DomainException
│   └── error/         # RestExceptionHandler
├── application/
├── domain/
│   ├── account/                 # BC Bank
│   │   ├── AccountRepository    # port
│   │   └── model/               # Account, Balance, Money, AccountStatus
│   └── exchange/                # BC Exchange
│       ├── order/
│       │   ├── OrderRepository  # port
│       │   └── model/           # Order, OrderSide, OrderType, OrderStatus
│       ├── orderbook/
│       │   ├── OrderBookRepository
│       │   └── model/           # OrderBook
│       ├── matching/            # Domain Service: OrderMatchingService, MatchResult, Trade
│       ├── trade/
│       │   ├── TradeRepository
│       │   └── model/           # ExecutedTrade
│       ├── shared/              # VO dùng chung: Price, Quantity, TradingPair
│       └── event/               # TradeExecutedEvent, DomainEventPublisher
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
# Nạp tiền — dùng accessToken của admin
curl -X POST http://localhost:8080/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_admin>" \
  -d '{"amount": 100000, "currency": "VND"}'

# Rút tiền — dùng accessToken của admin
curl -X POST http://localhost:8080/api/accounts/ACC-001/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_admin>" \
  -d '{"amount": 50000, "currency": "VND"}'

# Trader nạp tiền → 403 (dùng accessToken của trader1)
curl -X POST http://localhost:8080/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"amount": 100000, "currency": "VND"}'
```

> Place order đã reserve/settle ví. Chi tiết: **§14**.

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

> Thay `<accessToken_trader1>` / `<accessToken_readonly1>` bằng token lấy từ login.

**1) Bán 5 @ 60M → PENDING (hoặc FILLED nếu đã có mua)**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'
```

**2) Mua 10 @ 60M → khớp 5, PARTIALLY_FILLED + log Trade executed**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":60000000}'
```

**3) Mua @ 50M → không khớp, PENDING**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":50000000}'
```

**4) Bán 5 @ 60M → khớp hết phần mua còn lại**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'
```

**5) Cặp ETH/VND chưa mở → ORDER_BOOK_NOT_OPEN**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"ETH","quoteCurrency":"VND","quantity":1,"price":1000000}'
```

**6) Readonly đặt lệnh → FORBIDDEN**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_readonly1>" \
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
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'

# Refresh — dán refreshToken lấy từ login
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# Logout — dán accessToken
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <accessToken_trader1>"
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
# Sai mật khẩu → AUTH_FAILED
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"sai-mat-khau"}'

# Không token → UNAUTHORIZED
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":1,"price":60000000}'

# Refresh giả → REFRESH_TOKEN_INVALID
curl -X POST http://localhost:8080/api/auth/refresh \
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
| GET | `/api/orders/{id}` | ORDER_READ |
| DELETE | `/api/orders/{id}` | ORDER_CANCEL |
| GET | `/api/accounts/{id}` | ACCOUNT_READ |
| POST | `/api/accounts/*/deposit` | ACCOUNT_DEPOSIT |
| POST | `/api/accounts/*/withdraw` | ACCOUNT_WITHDRAW |

Chưa có controller: GET/POST order-books, GET trades list.

---

## 10. Kịch bản test đầy đủ (Postman)

Thứ tự: login → copy token → gọi từng API bên dưới.

```bash
# 1) Login trader1
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"password123"}'

# 2) Login admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# 3) Test lỗi login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader1","password":"sai"}'

# 4) Nạp tiền (Bearer accessToken của admin)
curl -X POST http://localhost:8080/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_admin>" \
  -d '{"amount":100000,"currency":"VND"}'

# 5) Đặt bán (Bearer accessToken của trader1)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-002","side":"SELL","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":5,"price":60000000}'

# 6) Đặt mua (Bearer accessToken của trader1)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken_trader1>" \
  -d '{"accountId":"ACC-001","side":"BUY","orderType":"LIMIT","baseCurrency":"BTC","quoteCurrency":"VND","quantity":10,"price":60000000}'

# 7) Logout
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <accessToken_trader1>"
```

---

## 11. Phân loại DDD

| Class | Loại | Package |
|-------|------|---------|
| Account | Aggregate Root | `domain.account.model` |
| Order, OrderBook | Aggregate Root | `domain.exchange.order.model` / `orderbook.model` |
| Money, Balance, Price, Quantity, Trade, TradingPair | Value Object | `account.model` / `exchange.shared` / `matching` |
| OrderMatchingService | Domain Service | `domain.exchange.matching` |
| MatchResult | Result object | `domain.exchange.matching` |
| TradeExecutedEvent | Domain Event | `domain.exchange.event` |
| *Repository | Port | cạnh `model/` của feature |
| PlaceOrderApplicationService | Application Service | `application` |
| JwtAuthFilter, resources, ErrorStatus | Infrastructure / API | `infrastructure` / `api` |

---

## 12. Lộ trình

### Đã xong

- [x] Account deposit/withdraw
- [x] Exchange domain + matching + TradeExecutedEvent
- [x] Login JWT + refresh (hash) + login_logs
- [x] RBAC + bảng `resources` (1 permission nhiều path)
- [x] Filter authz (không @PreAuthorize)
- [x] ErrorStatus / ApiResponse
- [x] Wallet / settlement (available+locked, reserve/settle/release)
- [x] Bảng `trades` + persist khi khớp
- [x] Cancel / GET order + GET account
- [x] Ownership (`users.account_id`)

### Chưa làm

- [ ] Register / change-password
- [ ] Hot reload resources (không restart)
- [ ] BUY MARKET (đang reject `MARKET_BUY_NOT_SUPPORTED`)
- [ ] Kafka / Outbox

**DB:** `scripts/init-full.sql` (schema + seed). Không còn file migrate cũ.

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
| `scripts/init-full.sql` | Schema + seed DB (file SQL duy nhất) |
| **`TAI-LIEU.md`** | **File tài liệu duy nhất** |

---

## 14. Hướng dẫn Wallet / Settlement / Cancel / Ownership

> **Đã implement.** Section này là cách nghĩ thiết kế (đọc để hiểu / phỏng vấn), không phải todo.

### 14.0 Hiện trạng (đã có trong code)

| Việc | Trạng thái |
|------|------------|
| Số dư Account | `account_balances`: available + locked, multi-currency |
| Place order | ownership → reserve → match → settle |
| Khớp lệnh | `Trade` RAM + persist bảng `trades` + event (log) |
| Cancel / GET order | API + release lock còn lại |
| Ownership | `users.account_id` (`trader1`→ACC-001, `trader2`→ACC-002) |
| DB | `scripts/init-full.sql` |

### 14.1 Quyết định thiết kế (đọc trước khi code)

**A. Số dư theo currency (bắt buộc vì BTC/VND)**

Một account giữ nhiều đồng: VND + BTC. Mỗi đồng có:

| Field | Nghĩa |
|-------|--------|
| `available` | Dùng được (nạp/rút/đặt lệnh mới) |
| `locked` | Đang treo trên lệnh chưa khớp xong |

```
available + locked = tổng nắm giữ
```

Gợi ý model domain:

```
Account
  └── Map<String currency, Balance> holdings
        Balance: available (Money), locked (Money)
```

Hoặc bảng phụ `account_balances (account_id, currency, available, locked)`.

**B. Matching vẫn “không biết tiền”**

`OrderMatchingService` **không** trừ tiền. Tiền nằm ở **Application**:

1. Place → `reserve`
2. Sau mỗi `Trade` → `settle`
3. Cancel → `release`

**C. Công thức reserve (LIMIT trước)**

| Side | Giữ gì | Số lượng lock |
|------|--------|----------------|
| **BUY LIMIT** | `quote` (VND) | `quantity × price` |
| **SELL LIMIT** | `base` (BTC) | `quantity` |

**MARKET (phase 1 đơn giản):**

| Side | Cách làm tạm |
|------|----------------|
| SELL MARKET | Lock toàn bộ `quantity` BTC như LIMIT |
| BUY MARKET | **Tạm chưa hỗ trợ** (không biết giá → không biết lock bao nhiêu VND) — trả lỗi rõ ràng, hoặc chỉ cho BUY LIMIT |

Sau này có thể: lock theo best ask × qty, hoặc “max spend” trong request.

**D. Settlement khi khớp 1 trade**

Ví dụ trade: buyer `ACC-001`, seller `ACC-002`, qty `Q`, price `P` (VND/BTC):

| Ai | Locked trước đó | Sau settle |
|----|-----------------|------------|
| Buyer | đã lock VND (có thể ≥ `Q×P` nếu LIMIT cao hơn giá khớp) | `consumeLocked(VND, Q×P)` + `credit(BTC, Q)`; **release** phần VND lock thừa nếu giá khớp &lt; giá limit |
| Seller | đã lock BTC | `consumeLocked(BTC, Q)` + `credit(VND, Q×P)` |

Định nghĩa method domain gợi ý:

| Method | Ý nghĩa |
|--------|---------|
| `reserve(currency, amount)` | `available → locked` (không đủ → lỗi) |
| `release(currency, amount)` | `locked → available` (cancel / thừa) |
| `consumeLocked(currency, amount)` | `locked` giảm, **không** về available (đã chi thật) |
| `credit(currency, amount)` | tăng `available` |

**E. Ownership**

- User có `users.account_id` (vd trader1 → ACC-001).
- Admin (`account_id` null) + permission `ACCOUNT_DEPOSIT` / … : có thể thao tác hộ (tuỳ rule).
- Trader: **chỉ** dùng đúng `account_id` của mình; order phải thuộc account đó.

Cách lấy user hiện tại:

```java
String username = (String) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
```

Rồi load `UserJpaEntity` / port `UserAccountResolver.resolveAccountId(username)`.

**F. Transaction**

Place / settle / cancel trong **một** `@Transactional` (cùng DB): save order + account + trade cùng commit. Chưa cần Kafka.

### 14.2 Luồng Place Order (đã implement)

```
ownership → reserve → match → settle (từng Trade) → save → publish event
```

Cancel: `order.cancel()` → gỡ sổ → `account.release(locked còn lại)`.

### 14.3 API wallet / exchange

| Method | Path | Permission |
|--------|------|------------|
| POST | `/api/orders` | ORDER_PLACE |
| DELETE | `/api/orders/{id}` | ORDER_CANCEL |
| GET | `/api/orders/{id}` | ORDER_READ |
| GET | `/api/accounts/{id}` | ACCOUNT_READ |

### 14.4 Pitfall thường gặp

| Lỗi | Cách tránh |
|-----|------------|
| Trừ available thẳng lúc place | Dùng `reserve` — cancel mới trả đúng |
| Settle trong domain matching | Matching không biết Account — settle ở application |
| Partial fill quên giảm locked | Settle theo **từng trade** (`Q×P`) |
| BUY LIMIT khớp giá thấp hơn | `consumeLocked(notional)` + `release` phần VND thừa |
| Tin `accountId` từ client | `OwnershipGuard` bắt buộc |
| BUY MARKET | Hiện reject `MARKET_BUY_NOT_SUPPORTED` |

### 14.5 Ví dụ số (test tay)

1. `ACC-001`: VND 10_000_000, BTC 5 (seed `init-full.sql`).
2. Trader1 BUY LIMIT 1 BTC @ 60_000_000 → locked VND 60_000_000.
3. Trader2 SELL LIMIT 1 BTC @ 60_000_000 → locked BTC 1.
4. Khớp: buyer nhận BTC, seller nhận VND; row trong `trades`.
5. Cancel lệnh treo: `locked` → `available`.

**Backlog:** Kafka/Outbox, register/password, GET order-books/trades, BUY MARKET.
