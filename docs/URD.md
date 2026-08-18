# URD — User Requirements Document

**Dự án:** Domain-Driven Design Bank + Exchange Demo  
**Phiên bản:** 1.0  
**Ngày:** 2026-08-18  
**Đối tượng:** BRD v1.0, SRS v1.0

---

## 1. Persona (Chân dung người dùng)

### P1 — Trader (trader1, trader2)

| Thuộc tính | Mô tả |
|---|---|
| Vai trò hệ thống | `ROLE_USER` |
| Account | Gắn cố định (vd ACC-001) |
| Mục tiêu | Mua/bán BTC bằng VND; quản lý ví |
| Pain point | Không được xem/sửa ví người khác; không nạp tiền (admin làm) |

### P2 — Admin (admin)

| Thuộc tính | Mô tả |
|---|---|
| Vai trò | `ROLE_ADMIN` |
| Account | Không gắn — thao tác mọi account |
| Mục tiêu | Nạp/rút demo; khóa ví vi phạm; hỗ trợ vận hành |
| Pain point | Cần quyền rõ ràng, tránh trader leo quyền |

### P3 — Readonly Auditor (readonly1)

| Thuộc tính | Mô tả |
|---|---|
| Vai trò | `ROLE_READONLY` |
| Mục tiêu | Xem account/order phục vụ kiểm tra |
| Hạn chế | Không đặt lệnh, không nạp/rút/transfer |

---

## 2. User Stories

### Auth

| ID | User story | Priority |
|---|---|---|
| US-A01 | Là user, tôi đăng nhập bằng username/password để nhận JWT. | Must |
| US-A02 | Là user, tôi refresh token khi access token hết hạn. | Must |
| US-A03 | Là user, tôi logout để vô hiệu refresh token. | Should |

### Wallet / Account

| ID | User story | Priority |
|---|---|---|
| US-W01 | Là trader, tôi xem số dư available/locked theo từng currency. | Must |
| US-W02 | Là admin, tôi nạp tiền vào account. | Must |
| US-W03 | Là admin/trader có quyền, tôi rút tiền từ available. | Must |
| US-W04 | Là trader, tôi chuyển tiền cùng currency sang account khác. | Must |
| US-W05 | Là admin, tôi khóa/mở khóa account (freeze/unfreeze). | Must |
| US-W06 | Là admin, tôi tạo account mới với số dư ban đầu. | Should |
| US-W07 | Là trader, tôi xem danh sách lệnh của account mình. | Must |

### Exchange

| ID | User story | Priority |
|---|---|---|
| US-E01 | Là trader, tôi đặt lệnh BUY/SELL LIMIT trên BTC/VND. | Must |
| US-E02 | Là trader, tôi đặt lệnh SELL MARKET. | Should |
| US-E03 | Là trader, tôi xem chi tiết một lệnh. | Must |
| US-E04 | Là trader, tôi hủy lệnh còn mở; tiền treo được trả lại. | Must |
| US-E05 | Là trader, tôi thấy lệnh khớp một phần hoặc toàn bộ. | Must |

### Security / UX lỗi

| ID | User story | Priority |
|---|---|---|
| US-S01 | Là user, tôi nhận lỗi JSON thống nhất (code + message) khi fail. | Must |
| US-S02 | Là readonly, tôi bị chặn 403 khi gọi API ghi. | Must |
| US-S03 | Là trader1, tôi không xem được account ACC-002. | Must |

---

## 3. Use Cases (Tóm tắt)

| UC | Tên | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC-01 | Login | Mọi user | Xác thực → JWT |
| UC-02 | Xem ví | Trader, Admin, Readonly | GET account + ownership |
| UC-03 | Nạp tiền | Admin | Deposit → available ↑ |
| UC-04 | Rút tiền | Admin / user có quyền | Withdraw từ available |
| UC-05 | Transfer | Trader | Chuyển cùng currency |
| UC-06 | Freeze/Unfreeze | Admin | Đổi status account |
| UC-07 | Đặt lệnh | Trader | Reserve → match → settle |
| UC-08 | Hủy lệnh | Trader | Cancel + release lock |
| UC-09 | Xem lệnh | Trader, Readonly | GET order + ownership |
| UC-10 | List orders by account | Trader | GET orders của account |

---

## 4. User Journey — Đặt lệnh và khớp (Happy path)

```
1. trader1 login → accessToken
2. admin login → deposit VND cho ACC-001 (nếu cần)
3. trader1 GET /accounts/ACC-001 → thấy available VND
4. trader2 đặt SELL LIMIT 1 BTC @ 61M (ACC-002)
5. trader1 đặt BUY LIMIT 1 BTC @ 61M (ACC-001)
   → VND reserve → khớp → settle
6. trader1 GET account → VND giảm, BTC tăng
7. trader1 GET order → status FILLED hoặc PARTIALLY_FILLED
8. trader1 GET /accounts/ACC-001/orders → thấy lệnh trong list
```

---

## 5. User Journey — Freeze account

```
1. admin login
2. admin POST freeze ACC-001
3. trader1 POST withdraw → lỗi (account frozen)
4. trader1 POST place order → lỗi (reserve fail)
5. admin POST unfreeze ACC-001
6. trader1 thao tác bình thường trở lại
```

---

## 6. Yêu cầu phi chức năng (góc người dùng)

| ID | Yêu cầu |
|---|---|
| U-NFR-01 | Thông báo lỗi bằng tiếng Việt, mã lỗi ổn định (vd `ACCOUNT_FROZEN`). |
| U-NFR-02 | Access token TTL ~15 phút; có refresh. |
| U-NFR-03 | API REST JSON; dùng được với Postman/curl. |

---

## 7. Ma trận Actor × Chức năng

| Chức năng | Trader | Admin | Readonly |
|---|:---:|:---:|:---:|
| Login / refresh / logout | ✓ | ✓ | ✓ |
| Xem account | ✓ (own) | ✓ (all) | ✓* |
| Nạp / rút | ✗ | ✓ | ✗ |
| Transfer | ✓ (from own) | ✓ | ✗ |
| Freeze / unfreeze | ✗ | ✓ | ✗ |
| Đặt / hủy lệnh | ✓ | ✓ | ✗ |
| Xem lệnh / list orders | ✓ (own) | ✓ | ✓ |

\* Readonly có permission READ; ownership vẫn áp dụng nếu gắn account.

---

## 8. Tài liệu liên quan

- [BRD.md](./BRD.md)
- [SRS.md](./SRS.md)
