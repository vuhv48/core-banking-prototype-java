# Hướng dẫn test nghiệp vụ bằng curl

Chạy app trước (cần Postgres + sổ BTC/VND đã seed):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Base URL: `http://localhost:8080`

Seed `dev` thường đã có sẵn lệnh trên sổ, ví dụ bán thấp nhất khoảng **61.000.000**.  
Sau mỗi lệnh, xem DB:

```sql
SELECT id, side, price, quantity, filled_quantity, status
FROM orders
ORDER BY created_at DESC
LIMIT 10;
```

Log console sẽ có `Trade executed: ...` khi khớp (Sprint 5).

---

## 0) Account — nạp / rút (Sprint 0, không liên quan khớp)

**Nạp tiền ACC-001**

```bash
curl -X POST http://localhost:8080/api/accounts/ACC-001/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 100000, "currency": "VND"}'
```

→ Tăng số dư tài khoản. Không tạo lệnh exchange.

**Rút tiền ACC-001**

```bash
curl -X POST http://localhost:8080/api/accounts/ACC-001/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 50000, "currency": "VND"}'
```

→ Giảm số dư (đủ tiền / không bị FROZEN).

---

## 1) Đặt lệnh BÁN — tạo thanh khoản trên sổ

Giả sử muốn tự dựng scenario sạch: bán 5 BTC @ 60.000.000 (Chị B).

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
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

**Nghiệp vụ:** Nếu trên sổ chưa có mua @ ≥ 60M → lệnh **PENDING**, nằm phía bán chờ.  
Response: `"status":"PENDING"` (hoặc FILLED nếu đã có mua khớp sẵn).

---

## 2) Đặt lệnh MUA khớp một phần (A mua 10, B đã bán 5)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
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

**Nghiệp vụ:** Giá mua ≥ giá bán → khớp **5**.  
- Lệnh bán 5 → **FILLED**  
- Lệnh mua → **PARTIALLY_FILLED**, còn 5 chờ trên sổ  
- Log: `Trade executed` (quantity 5, price 60000000)

---

## 3) Không khớp — giá mua thấp hơn giá bán

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
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

**Nghiệp vụ:** Mua max 50M, bán đang ≥ 60M → **không khớp**.  
Response: `"status":"PENDING"`. Không có log Trade executed cho lần này.

---

## 4) Khớp hết — mua đúng khối lượng còn lại

Nếu sau bước 2 còn 5 mua chờ @ 60M, đặt bán 5 @ 60M:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
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

**Nghiệp vụ:** Khớp hết 5 → lệnh bán mới **FILLED**; lệnh mua còn lại cũng **FILLED**.

---

## 5) Cặp chưa mở — bị reject

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
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

**Nghiệp vụ:** Không có `order_books` ETH/VND → lỗi *"Cặp giao dịch chưa được mở"*.

---

## 6) LIMIT thiếu giá — bị reject

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC-001",
    "side": "BUY",
    "orderType": "LIMIT",
    "baseCurrency": "BTC",
    "quoteCurrency": "VND",
    "quantity": 1,
    "price": null
  }'
```

**Nghiệp vụ:** LIMIT bắt buộc có giá → lỗi từ domain `Order`.

---

## Gợi ý thứ tự test hiểu nghiệp vụ

1. Lệnh **1** (bán 5 @ 60M) → PENDING  
2. Lệnh **2** (mua 10 @ 60M) → PARTIALLY_FILLED + Trade log  
3. Lệnh **3** (mua @ 50M) → PENDING, không trade  
4. Lệnh **4** (bán 5 @ 60M) → FILLED  
5. Lệnh **5** / **6** → xem rule reject  

Song song chạy `mvn test -Dtest=OrderMatchingServiceTest` để củng cố logic không cần API.
