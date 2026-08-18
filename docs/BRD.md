
Thứ tự đọc (khoảng 2–3 buổi)
Buổi 1 — Hiểu “cái gì, cho ai” (~1 giờ)
Thứ tự	File	Đọc gì	Bỏ qua
1
docs/BRD.md
§1 Tóm tắt, §3 Scope, §5 Business Rules (BR-01→BR-10)
Chi tiết stakeholder
2
docs/URD.md
§1 Persona, §4 User Journey “Đặt lệnh và khớp”, §5 Journey Freeze
Bảng user story dài
Sau buổi 1 trả lời được:

Bank vs Exchange khác gì?
available vs locked là gì?
Freeze làm gì?
Buổi 2 — Hiểu “phần mềm bắt buộc làm gì” (~1 giờ)
Thứ tự	File	Đọc gì
3
docs/SRS.md
§1 Kiến trúc (sơ đồ 8 dòng), §2 FR (lướt tiêu đề), §7 Place Order
4
SENIOR-NOTES.md
A0 Vòng 1 (luồng tiền) — tự điền ghi chú, không chỉ đọc
Sau buổi 2: vẽ được dòng deposit → reserve → match → settle → withdraw.

Buổi 3 — Map sang code (~1–2 giờ)
Thứ tự	File	Đọc gì
5
TAI-LIEU.md
§1 Dự án là gì, §2 Kiến trúc, §6 Luồng Place Order, §14 Wallet/Settlement
6
Code (chỉ 4 file)
Xem theo thứ tự bên dưới
4 file code nên mở đầu tiên:

AccountController.java — API ví
PlaceOrderApplicationService.java — xương sống exchange
domain/account/model/Account.java — rule tiền
domain/exchange/order/model/Order.java — rule lệnh
Đừng đọc hết infrastructure/ lúc đầu.

Không nên đọc sớm
File	Vì sao
Toàn bộ SRS FR từng dòng
Giống catalog, dễ mệt
SENIOR-NOTES phần C/E/F
Test/feature — sau khi hiểu hệ thống
JPA / Security filter chi tiết
Sau khi hiểu luồng chính
Cách đọc khi code do AI gen
Đọc doc → đoán flow → mở 1 file confirm (không đọc code trước doc).
Mỗi API hỏi 3 câu: Ai gọi? Application nào? Domain method nào?
Làm A0 Vòng 1 trong SENIOR-NOTES — viết tay, gửi mình chấm.
Lộ trình 1 tuần (nhẹ)
Ngày	Việc
1
BRD + URD (buổi 1)
2
SRS §1,§7 + A0 Vòng 1
3
TAI-LIEU §2,§6 + PlaceOrderApplicationService
4
A0 Vòng 2 (quyền) + chạy curl login → get account
5
A0 Vòng 3 + A1 (viết place order 7 bước)
Bắt đầu ngay: mở docs/BRD.md §1 và §5 (10 business rules). Đọc xong kể lại bằng lời của bạn: “Hệ thống này làm gì?” — 5–6 câu, gửi mình, mình chỉ chỗ còn mơ hồ.

# BRD — Business Requirements Document

**Dự án:** Domain-Driven Design Bank + Exchange Demo  
**Phiên bản:** 1.0  
**Ngày:** 2026-08-18  
**Trạng thái:** Demo / Portfoliot

---

## 1. Tóm tắt điều hành

Hệ thống mô phỏng **ví đa tiền tệ (Bank/Wallet)** kết hợp **sàn giao dịch spot đơn giản (Exchange)** cho cặp BTC/VND. Mục đích chính: học và trình diễn **Domain-Driven Design (DDD)** + **Hexagonal Architecture** trên stack Spring Boot, không phải sản phẩm ngân hàng/sàn thật.

**Giá trị nghiệp vụ (demo):**

- Quản lý số dư theo từng loại tiền (available / locked).
- Đặt lệnh mua/bán, khớp lệnh LIMIT, tất toán ví hai bên.
- Phân quyền người dùng (trader, admin, readonly) qua JWT + RBAC.

---

## 2. Mục tiêu kinh doanh

| ID | Mục tiêu | Chỉ số thành công (demo) |
|---|---|---|
| BG-01 | Mô phỏng ví multi-currency | Nạp/rút/transfer; available + locked chính xác |
| BG-02 | Mô phỏng sàn spot cơ bản | Đặt lệnh LIMIT, khớp, lưu lịch sử trade |
| BG-03 | Bảo vệ tài sản khi treo lệnh | Reserve trước khi khớp; release khi hủy |
| BG-04 | Kiểm soát truy cập | User chỉ thao tác account/order của mình (trừ admin) |
| BG-05 | Minh họa DDD | Rule nghiệp vụ nằm Domain; API mỏng |

---

## 3. Phạm vi (Scope)

### 3.1 Trong phạm vi (In scope)

| Lĩnh vực | Nội dung |
|---|---|
| **Wallet** | Account multi-currency; deposit; withdraw; transfer nội bộ; freeze/unfreeze |
| **Exchange** | Place order BUY/SELL LIMIT; SELL MARKET; matching price-time; cancel order |
| **Settlement** | Trừ/cộng ví sau khớp; lưu bảng `trades` |
| **Security** | Login JWT; refresh token; logout; RBAC qua bảng `resources` |
| **Audit** | Login logs; lịch sử order/trade trên DB |

### 3.2 Ngoài phạm vi (Out of scope)

- KYC/AML, tuân thủ pháp lý tài chính thật.
- Thanh toán ngân hàng / blockchain on-chain.
- BUY MARKET (chưa hỗ trợ — reject có mã lỗi).
- Đăng ký user, đổi mật khẩu (backlog).
- Order book public API, list trades API (backlog).
- Kafka, Outbox, multi-region, HA.
- Phí giao dịch, thuế, FX rate ngoài sàn.

---

## 4. Stakeholder

| Vai trò | Mô tả | Nhu cầu chính |
|---|---|---|
| **Trader (ROLE_USER)** | Người giao dịch gắn 1 account | Đặt/hủy/xem lệnh; xem ví; transfer |
| **Admin (ROLE_ADMIN)** | Vận hành hệ thống | Nạp/rút hộ; freeze account; toàn quyền |
| **Readonly (ROLE_READONLY)** | Kiểm tra/audit | Chỉ xem account/order |
| **Developer / Học viên** | Xây dựng & học DDD | Code rõ layer, invariant trong domain |
| **Reviewer / Interviewer** | Đánh giá portfolio | Doc, curl, kiến trúc giải thích được |

---

## 5. Quy tắc nghiệp vụ (Business Rules)

| ID    | Quy tắc |
|---    |---      |
| BR-01 | Mỗi account có nhiều dòng số dư theo `currency`; mỗi dòng có `available` và `locked`. |
| BR-02 | Rút/chuyển/đặt lệnh chỉ dùng `available`; phần `locked` chỉ giải phóng qua cancel hoặc settle. |
| BR-03 | Account **FROZEN** không được rút (`withdraw`) và không được treo lệnh (`reserve`); vẫn cho **deposit**. |
| BR-04 | BUY LIMIT: treo VND = `price × quantity`. SELL LIMIT/MARKET: treo BTC = `quantity`. |
| BR-05 | Khớp lệnh: giá BUY ≥ giá SELL (LIMIT); cập nhật `filledQuantity`; partial fill được phép. |
| BR-06 | Sau khớp: buyer nhận BTC, seller nhận VND; tiền treo được `consumeLocked` / `credit` đúng bên. |
| BR-07 | Hủy lệnh chưa kết thúc: release phần lock còn lại về `available`. |
| BR-08 | Transfer: cùng currency; amount > 0; không chuyển sang chính account nguồn. |
| BR-09 | User gắn `account_id` chỉ thao tác account đó; admin không gắn account → thao tác mọi account. |
| BR-10 | BUY MARKET bị từ chối (`MARKET_BUY_NOT_SUPPORTED`) — chưa xác định VND cần giữ. |

---

## 6. Ràng buộc & giả định

**Giả định:**

- Một user trader ↔ tối đa một account (demo).
- Số tiền lưu `long` (đơn vị nhỏ nhất — demo không chia satoshi/đồng lẻ phức tạp).
- Một instance app + PostgreSQL; không cluster.
- Seed data qua `scripts/init-full.sql`.

**Ràng buộc kỹ thuật:**

- Spring Boot 3, Java, PostgreSQL.
- Domain không phụ thuộc Spring Security / JPA.
- Permission map qua DB (`resources`), load lúc startup.

---

## 7. Tiêu chí chấp nhận cấp nghiệp vụ

- [ ] Trader đặt lệnh BUY LIMIT thành công khi đủ VND available; VND chuyển sang locked.
- [ ] Hai lệnh đối ứng khớp → số dư hai ví đúng; bản ghi `trades` được tạo.
- [ ] Hủy lệnh PENDING → locked trả về available.
- [ ] Admin freeze account → trader không rút/đặt lệnh được.
- [ ] Transfer giữa hai account → tổng số dư hệ thống không đổi (conservation).
- [ ] User không sở hữu account → 403 `ACCOUNT_NOT_OWNED`.

---

## 8. Thuật ngữ

| Thuật ngữ | Ý nghĩa |
|---|---|
| **Available** | Số dư dùng được ngay |
| **Locked** | Số dư đang treo trên lệnh |
| **Order** | Lệnh mua/bán (có thể chưa khớp hết) |
| **Trade** | Một lần khớp giữa hai lệnh |
| **Settlement** | Cập nhật ví sau khi khớp |
| **Reserve** | Chuyển available → locked khi đặt lệnh |

---

## 9. Tài liệu liên quan

- [URD.md](./URD.md) — yêu cầu người dùng
- [SRS.md](./SRS.md) — đặc tả phần mềm
- [../TAI-LIEU.md](../TAI-LIEU.md) — tài liệu kỹ thuật & curl
- [../SENIOR-NOTES.md](../SENIOR-NOTES.md) — lộ trình học senior
