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
14. [Hướng dẫn Wallet / Settlement / Cancel / Ownership](#14-hướng-dẫn-wallet--settlement--cancel--ownership)

---

## 0. Chuẩn bị & login

### Chạy app

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Cần PostgreSQL, database `account_demo`. Profile `dev` load `data.sql`.
Base URL: `http://localhost:8080`

### User mẫu

| Username | Password | Role | Dùng để |
|----------|----------|------|---------|
| `admin` | `password123` | ROLE_ADMIN | Nạp/rút, toàn quyền |
| `trader1` | `password123` | ROLE_USER | Đặt lệnh |
| `readonly1` | `password123` | ROLE_READONLY | Test 403 |

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

> Exchange chưa trừ số dư Account khi đặt lệnh. Hướng dẫn làm: **§14**.

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
| POST | `/api/accounts/*/deposit` | ACCOUNT_DEPOSIT |
| POST | `/api/accounts/*/withdraw` | ACCOUNT_WITHDRAW |

Đã map trong `resources` nhưng chưa có controller: GET/DELETE orders, GET/POST order-books, GET accounts.

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
- [x] Wallet / settlement (available+locked, reserve/settle/release)
- [x] Bảng `trades` + persist khi khớp
- [x] Cancel / GET order + GET account
- [x] Ownership (`users.account_id`)

### Chưa làm

- [ ] Register / change-password
- [ ] Hot reload resources (không restart)
- [ ] BUY MARKET (đang reject `MARKET_BUY_NOT_SUPPORTED`)
- [ ] Kafka / Outbox

**DB cũ:** chạy `scripts/migrate-wallet-settlement.sql` rồi restart.

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

---

## 14. Hướng dẫn Wallet / Settlement / Cancel / Ownership

> **Đã implement trong code.** Section này vẫn là tài liệu thiết kế / cách nghĩ.
> Migration DB cũ: `scripts/migrate-wallet-settlement.sql`.

### 14.0 Hiện trạng (gap)

| Việc | Hiện tại |
|------|----------|
| Số dư Account | 1 field `balance` (VND) — **không** có locked / multi-currency |
| Place order | Không đụng Account; `accountId` lấy từ body |
| Khớp lệnh | `Trade` trên RAM + `TradeExecutedEvent` **chỉ log** |
| Cancel / GET order | Domain `Order.cancel()` có sẵn; **chưa** app service / API |
| Bảng `trades` | **Chưa có** |
| Ownership | `users.account_id` đã seed (`trader1` → `ACC-001`) nhưng **không dùng** khi đặt lệnh |

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

---

### 14.2 Checklist làm theo Phase

#### Phase 0 — Chuẩn bị ErrorStatus / test seed

1. Thêm mã lỗi (gợi ý): `INSUFFICIENT_BALANCE`, `ORDER_NOT_FOUND`, `ORDER_NOT_OWNED`, `ACCOUNT_NOT_OWNED`, `MARKET_BUY_NOT_SUPPORTED`, `ORDER_NOT_CANCELLABLE`.
2. Seed: `ACC-001` có VND + một ít BTC (để test SELL); `trader1.account_id = ACC-001`.
3. Viết kịch bản test tay (Postman) vào cuối §14.5 — chưa cần code hết.

#### Phase 1 — Domain Account: holdings + reserve/release/consume/credit

**Layer:** `domain/account` + persistence + migrate.

1. Refactor `Account`: bỏ single `balance` → holdings theo currency (hoặc giữ `getBalance()` = available VND để tương thích tạm, rồi migrate hết).
2. Implement 4 method trên; rule: FROZEN không reserve/withdraw.
3. Cập nhật `deposit` / `withdraw` chỉ đụng `available`.
4. JPA: bảng `account_balances` **hoặc** cột `locked_*` + multi-row.
5. Script migrate + cập nhật `data.sql`.
6. Unit test domain: reserve hết available → fail; release đúng; consumeLocked không trả available.

**Không** sửa PlaceOrder ở phase này.

#### Phase 2 — Ownership helper

**Layer:** application + (optional) infrastructure.

1. Port: `CurrentUserAccountPort` / service `OwnershipGuard`:
   - `requireAccountAccess(username, accountId)`
   - `requireOrderAccess(username, order)` — so `order.accountId`
2. Rule gợi ý:
   - User có `account_id` → chỉ được đúng id đó.
   - User `account_id == null` và có quyền admin (vd `ACCOUNT_DEPOSIT`) → được bypass (hoặc tách permission `ACCOUNT_IMPERSONATE` sau).
3. Gắn vào **PlaceOrder** trước (reject body `accountId` lệch).
4. Có thể bỏ `accountId` khỏi request body sau này — lấy từ user đang login. Phase 2: vẫn nhận body nhưng **bắt buộc khớp**.

#### Phase 3 — Place order = reserve rồi match

**File chính:** `PlaceOrderApplicationService`.

Thứ tự trong 1 transaction:

```
1. Ownership check (accountId)
2. Load Account
3. Nếu BUY MARKET → reject (phase đầu)
4. Tính lockAmount + lockCurrency (công thức §14.1.C)
5. account.reserve(...)
6. accountRepository.save
7. (giữ nguyên) match + save orders + orderBook
8. Với mỗi Trade → settle (Phase 4) rồi publish event
9. Return order
```

Lưu trên Order (khuyến nghị): `lockedCurrency`, `lockedAmount` (hoặc tính lại từ remaining × price) để cancel/release đúng. LIMIT BUY lock theo **giá limit**; khi khớp giá thấp hơn phải release phần thừa (Phase 4).

#### Phase 4 — Settle khi khớp + bảng `trades`

**Application:** `TradeSettlementService` (gọi từ PlaceOrder sau match, hoặc từ listener in-process của `DomainEventPublisher`).

Với mỗi `Trade`:

1. Load buyOrder / sellOrder → lấy `buyerAccountId`, `sellerAccountId`.
2. `notional = qty × price` (VND).
3. Buyer: `consumeLocked(VND, notional)` (+ `release` VND thừa nếu lock theo limit cao hơn); `credit(BTC, qty)`.
4. Seller: `consumeLocked(BTC, qty)`; `credit(VND, notional)`.
5. Save cả 2 account.
6. Persist bảng `trades` (xem schema dưới).
7. Enrich `TradeExecutedEvent` (optional): thêm account ids — tiện log/Kafka sau.

**Schema gợi ý `trades`:**

```sql
CREATE TABLE trades (
  id              VARCHAR(64) PRIMARY KEY,
  buy_order_id    VARCHAR(64) NOT NULL,
  sell_order_id   VARCHAR(64) NOT NULL,
  trading_pair    VARCHAR(32) NOT NULL,  -- hoặc base/quote
  quantity        BIGINT NOT NULL,
  price           BIGINT NOT NULL,
  buyer_account_id  VARCHAR(64) NOT NULL,
  seller_account_id VARCHAR(64) NOT NULL,
  created_at      TIMESTAMP NOT NULL,
  ...
);
```

Matching vẫn tạo `Trade` VO; application map → entity rồi save.

#### Phase 5 — Cancel + release

Domain đã có `Order.cancel()` — làm tiếp:

1. `CancelOrderApplicationService.cancel(orderId, username)`:
   - Load order; ownership; status phải PENDING hoặc PARTIALLY_FILLED.
   - `order.cancel()`.
   - Gỡ khỏi `OrderBook` nếu còn trên sổ.
   - Tính remaining locked:
     - SELL: remaining qty BTC
     - BUY LIMIT: remaining qty × limit price (trừ phần đã consume khi partial fill — cần track `lockedRemaining` hoặc suy từ filled).
   - `account.release(...)`.
   - Save order, book, account.
2. API: `DELETE /api/orders/{orderId}` — permission `ORDER_CANCEL` đã map trong `resources`.

#### Phase 6 — GET orders (+ GET account)

1. `GET /api/orders/{orderId}` và/hoặc `GET /api/orders?accountId=...`
2. Ownership: trader chỉ xem order của mình; admin xem được nhiều hơn (tuỳ chọn).
3. `GET /api/accounts/{accountId}` — trả available/locked theo currency; permission `ACCOUNT_READ` đã có.
4. (Optional) `GET /api/trades?orderId=` — sau khi có bảng trades.

---

### 14.3 File / class nên thêm hoặc sửa

| Layer | Thêm / sửa |
|-------|------------|
| domain/account | `Balance`, `reserve` / `release` / `consumeLocked` / `credit`; refactor `Account` |
| domain/exchange | (optional) field lock trên `Order`; enrich `TradeExecutedEvent` |
| application | `OwnershipGuard`, sửa `PlaceOrderApplicationService`, `TradeSettlementService`, `CancelOrderApplicationService`, `GetOrderApplicationService` |
| infrastructure | JPA `account_balances`, `trades`; migrate SQL; `UserAccountResolver` |
| api | `DELETE/GET` orders; `GET` account; bỏ hoặc siết `accountId` trong place body |
| common | `ErrorStatus` mới |
| test | Unit Account; integration place → balance đổi; cancel → release |

**Giữ nguyên:** `OrderMatchingService` không import Account.

---

### 14.4 Thứ tự commit / test gợi ý

```
Phase 1  → unit test Account holdings
Phase 2  → place với accountId người khác → 403
Phase 3+4→ BUY LIMIT + SELL LIMIT khớp → VND/BTC 2 bên đổi đúng; row trong trades
Phase 5  → cancel phần chưa khớp → locked về available
Phase 6  → GET thấy đúng; readonly vẫn 403 place
```

**Ví dụ số (dùng khi test):**

1. `ACC-001`: available VND = 10_000_000; BTC = 2 (seed thêm BTC).
2. Trader1 BUY LIMIT 1 BTC @ 1_000_000 → available VND 9_000_000, locked VND 1_000_000.
3. ACC-002 SELL LIMIT 1 BTC @ 1_000_000 (cần user/account tương ứng hoặc admin đặt hộ trong test).
4. Khớp: ACC-001 available BTC += 1, locked VND = 0; ACC-002 available VND += 1_000_000, locked BTC giảm.
5. Cancel lệnh treo: locked về available.

---

### 14.5 API sau khi làm xong (kỳ vọng)

| Method | Path | Permission | Ghi chú |
|--------|------|------------|---------|
| POST | `/api/orders` | ORDER_PLACE | Reserve + match + settle |
| DELETE | `/api/orders/{id}` | ORDER_CANCEL | Cancel + release |
| GET | `/api/orders/{id}` | ORDER_READ | Ownership |
| GET | `/api/accounts/{id}` | ACCOUNT_READ | available/locked |

Kafka: **chưa làm** ở epic này. Khi settle ổn định, mới cân nhắc `KafkaDomainEventPublisher` thay / kèm `LoggingDomainEventPublisher`.

---

### 14.6 Pitfall thường gặp

| Lỗi | Cách tránh |
|-----|------------|
| Trừ `balance` thẳng lúc place, không lock | Dùng `available`/`locked` — cancel mới đúng |
| Settle trong domain matching | Matching không biết Account — settle ở application |
| Partial fill quên giảm locked đúng số | Settle theo **từng trade** (`Q×P`), không release hết một lần |
| BUY LIMIT khớp giá thấp hơn | `consumeLocked(notional)` + `release(phần thừa còn lại của phần qty đó)` |
| Tin `accountId` từ client | Ownership bắt buộc (Phase 2) |
| MARKET BUY lock sai | Chặn tạm hoặc yêu cầu max VND |
| Event log rồi settle async không cùng TX | Phase này settle **đồng bộ** trong cùng transaction |

---

### 14.7 Định nghĩa xong / chưa

Xong epic này khi:

- [ ] Place LIMIT trừ/giữ đúng available & locked
- [ ] Khớp lệnh → 2 account đổi VND/BTC đúng; có row `trades`
- [ ] Cancel → trả locked còn lại
- [ ] Trader không đặt/xem/huỷ lệnh account khác
- [ ] GET order + GET account chạy với permission hiện có
- [ ] Cập nhật §12 checklist + ví dụ curl §9/§10

**Bước tiếp theo sau epic:** Kafka/Outbox, register/password, hot-reload resources — không gộp vào đây.
