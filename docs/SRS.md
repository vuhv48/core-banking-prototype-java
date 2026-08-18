# SRS — Software Requirements Specification

**Dự án:** Domain-Driven Design Bank + Exchange Demo  
**Phiên bản:** 1.0  
**Ngày:** 2026-08-18  
**Stack:** Java, Spring Boot 3, PostgreSQL, JWT

---

## 1. Tổng quan hệ thống

### 1.1 Mục đích

REST API backend mô phỏng ví đa tiền tệ và sàn spot BTC/VND, triển khai theo **DDD + Hexagonal Architecture**.

### 1.2 Kiến trúc logic

```
Client (HTTP/JSON)
  → JwtAuthFilter
  → AuthorizationFilter (resources DB)
  → Controller (api/)
  → Application Service (application/)
  → Domain (domain/) — pure Java
  → Repository Port → JPA Adapter (infrastructure/)
  → PostgreSQL
```

### 1.3 Bounded Context

| Context | Aggregate chính | Package |
|---|---|---|
| Bank / Wallet | `Account` | `domain.account` |
| Exchange | `Order`, `OrderBook` | `domain.exchange` |

---

## 2. Yêu cầu chức năng (Functional Requirements)

### 2.1 Authentication & Authorization

| ID | Mô tả | API | Permission |
|---|---|---|---|
| FR-A01 | Login trả access + refresh token + permissions | `POST /api/auth/login` | public |
| FR-A02 | Refresh access token | `POST /api/auth/refresh` | public |
| FR-A03 | Logout thu hồi refresh token | `POST /api/auth/logout` | JWT |
| FR-A04 | Map HTTP method + path → permission qua bảng `resources` | filter | — |
| FR-A05 | JWT chứa claim `permissions` | JwtUtil | — |
| FR-A06 | Ownership: user.account_id khớp account thao tác | OwnershipChecker | — |

### 2.2 Account / Wallet

| ID | Mô tả | API | Permission |
|---|---|---|---|
| FR-W01 | Xem account (holdings available/locked) | `GET /api/accounts/{id}` | ACCOUNT_READ |
| FR-W02 | Tạo account + holdings ban đầu | `POST /api/accounts` | —* |
| FR-W03 | Nạp tiền (available ↑) | `POST /api/accounts/{id}/deposit` | ACCOUNT_DEPOSIT |
| FR-W04 | Rút tiền (available ↓, cần ACTIVE) | `POST /api/accounts/{id}/withdraw` | ACCOUNT_WITHDRAW |
| FR-W05 | Transfer nội bộ cùng currency | `POST /api/accounts/transfer` | ACCOUNT_WITHDRAW |
| FR-W06 | Freeze account (ACTIVE → FROZEN) | `POST /api/accounts/{id}/freeze` | ACCOUNT_FREEZE |
| FR-W07 | Unfreeze (FROZEN → ACTIVE) | `POST /api/accounts/{id}/unfreeze` | ACCOUNT_FREEZE |
| FR-W08 | List orders theo account | `GET /api/accounts/{id}/orders` | ACCOUNT_READ |

\* Create account: chưa map resource đầy đủ trong seed — có thể 403 tùy cấu hình.

### 2.3 Exchange / Order

| ID | Mô tả | API | Permission |
|---|---|---|---|
| FR-E01 | Place order LIMIT/MARKET (SELL MARKET OK; BUY MARKET reject) | `POST /api/orders` | ORDER_PLACE |
| FR-E02 | Reserve trước khi vào sổ lệnh | Application + Account.reserve | — |
| FR-E03 | Matching LIMIT qua OrderMatchingService | Domain | — |
| FR-E04 | Settlement qua TradeSettlementService | Application | — |
| FR-E05 | Persist ExecutedTrade vào bảng `trades` | TradeRepository | — |
| FR-E06 | Xem chi tiết order | `GET /api/orders/{id}` | ORDER_READ |
| FR-E07 | Hủy order + release lock | `DELETE /api/orders/{id}` | ORDER_CANCEL |
| FR-E08 | Publish TradeExecutedEvent sau khớp | DomainEventPublisher | — |

### 2.4 Domain Invariants (phần mềm phải đảm bảo)

| ID | Invariant | Vị trí |
|---|---|---|
| FR-D01 | available không âm | `Balance`, `Account` |
| FR-D02 | FROZEN → không withdraw/reserve | `Account.ensureActiveForDebit` |
| FR-D03 | Không match/cancel order final | `Order` |
| FR-D04 | Match không vượt remaining quantity | `Order.match` |
| FR-D05 | LIMIT order phải có price | `Order` constructor |
| FR-D06 | Transfer: from ≠ to, amount > 0 | TransferApplicationService |
| FR-D07 | freeze/unfreeze idempotent guard | `Account.freeze/unfreeze` |

---

## 3. Yêu cầu phi chức năng (Non-Functional Requirements)

| ID | Loại | Mô tả |
|---|---|---|
| NFR-01 | Security | Mật khẩu BCrypt; JWT signed; refresh token hash trong DB |
| NFR-02 | Security | CSRF disabled (stateless API) |
| NFR-03 | API | Response lỗi/thành công envelope `ApiResponse` + `ErrorStatus` |
| NFR-04 | Persistence | PostgreSQL; soft delete (`deleted`) trên entity chính |
| NFR-05 | Transaction | Use case ghi (`@Transactional`); transfer/freeze/place order atomic |
| NFR-06 | Maintainability | Domain không import Spring/JPA |
| NFR-07 | Observability | Login logs; startup log load resources |
| NFR-08 | Performance | Demo — không yêu cầu SLA; single instance |
| NFR-09 | i18n | Message lỗi tiếng Việt |

---

## 4. Mô hình dữ liệu (tóm tắt)

### 4.1 Bảng chính

| Bảng | Mô tả |
|---|---|
| `accounts` | accountId, status (ACTIVE/FROZEN) |
| `account_balances` | account_id, currency, available, locked |
| `orders` | lệnh giao dịch + lock còn lại |
| `trades` | lịch sử khớp (ExecutedTrade) |
| `users`, `roles`, `permissions`, `role_permissions`, `resources` | RBAC |
| `refresh_tokens`, `login_logs` | Auth audit |

### 4.2 Seed

- Script: `scripts/init-full.sql`
- Users: admin, trader1 (ACC-001), trader2 (ACC-002), readonly1
- Sample orders + TRD-001 (ORD-BUY-HIST ↔ ORD-SELL-HIST)

---

## 5. Giao diện bên ngoài (API Contract)

Base URL: `http://localhost:8080`

Header bảo vệ: `Authorization: Bearer <accessToken>`

### 5.1 Auth

| Method | Path | Body |
|---|---|---|
| POST | `/api/auth/login` | `{username, password}` |
| POST | `/api/auth/refresh` | `{refreshToken}` |
| POST | `/api/auth/logout` | JWT header |

### 5.2 Account

| Method | Path | Body / Note |
|---|---|---|
| GET | `/api/accounts/{accountId}` | → AccountResponse |
| POST | `/api/accounts` | AccountRequest |
| POST | `/api/accounts/{id}/deposit` | `{amount, currency}` |
| POST | `/api/accounts/{id}/withdraw` | `{amount, currency}` |
| POST | `/api/accounts/transfer` | `{fromAccountId, toAccountId, amount, currency}` |
| GET | `/api/accounts/{id}/orders` | → List OrderResponse |
| POST | `/api/accounts/{id}/freeze` | — |
| POST | `/api/accounts/{id}/unfreeze` | — |

### 5.3 Order

| Method | Path | Body |
|---|---|---|
| POST | `/api/orders` | PlaceOrderRequest |
| GET | `/api/orders/{orderId}` | → OrderResponse |
| DELETE | `/api/orders/{orderId}` | → OrderResponse |

Chi tiết curl: [TAI-LIEU.md §9](../TAI-LIEU.md)

---

## 6. Xử lý lỗi

| HTTP | Code ví dụ | Khi nào |
|---|---|---|
| 400 | INVALID_ARGUMENT, INSUFFICIENT_BALANCE | Rule/input |
| 401 | UNAUTHORIZED, AUTH_FAILED | Chưa login / sai pass |
| 403 | FORBIDDEN, ACCOUNT_NOT_OWNED, ACCOUNT_FROZEN | Permission/ownership |
| 404 | ACCOUNT_NOT_FOUND, ORDER_NOT_FOUND | Không tồn tại |
| 409 | ILLEGAL_STATE, ORDER_NOT_CANCELLABLE | Trạng thái không hợp lệ |

Domain throw `IllegalStateException` / `IllegalArgumentException` → `RestExceptionHandler` map sang `ErrorStatus`.

---

## 7. Luồng phần mềm — Place Order (reference)

1. `OrderController` → map DTO → VO domain  
2. `PlaceOrderApplicationService.placeOrder`  
3. `OwnershipChecker.requireAccountAccess`  
4. Load `Account` → tính số cần reserve → `account.reserve` → save  
5. Tạo `Order` → `order.initializeLock` → save  
6. Load `OrderBook` → `OrderMatchingService.match`  
7. Với mỗi fill: `TradeSettlementService.settle` → save accounts, orders, trades  
8. Publish event (nếu có)  
9. Save orderbook  

---

## 8. Phụ thuộc & công nghệ

| Thành phần | Công nghệ |
|---|---|
| Runtime | Java 17+ |
| Framework | Spring Boot 3.3 |
| ORM | Spring Data JPA / Hibernate |
| DB | PostgreSQL |
| Security | Spring Security + JWT (jjwt) |
| Build | Maven |

---

## 9. Hạn chế phiên bản hiện tại

| Hạn chế | Ghi chú |
|---|---|
| BUY MARKET | Reject `MARKET_BUY_NOT_SUPPORTED` |
| Register / change password | Chưa có |
| GET order-books, GET trades list | Chưa có API |
| Resources reload | Cần restart app sau INSERT |
| Create account permission | Có thể thiếu resource rule |
| Không optimistic lock | Race condition withdraw đồng thời |

---

## 10. Traceability (BRD → SRS)

| BRD | SRS |
|---|---|
| BR-01..03 (wallet) | FR-W*, FR-D01..02 |
| BR-04..07 (exchange) | FR-E*, FR-D03..05 |
| BR-08 (transfer) | FR-W05, FR-D06 |
| BR-09 (ownership) | FR-A06 |
| BR-10 (no BUY MARKET) | FR-E01, §9 |

---

## 11. Tài liệu liên quan

- [BRD.md](./BRD.md)
- [URD.md](./URD.md)
- [../TAI-LIEU.md](../TAI-LIEU.md) — chi tiết triển khai
- [../SENIOR-NOTES.md](../SENIOR-NOTES.md) — bài tập học
