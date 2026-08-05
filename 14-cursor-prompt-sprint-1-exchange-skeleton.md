# Prompt cho Cursor — Dựng khung sườn Sprint 1 (Exchange domain, Hexagonal Architecture)

**Cách dùng**: copy nguyên khối trong ô code bên dưới, dán vào Cursor (chat/agent mode), chạy trong project Spring Boot đã có sẵn từ Sprint 0 (Account). Cursor sẽ tạo thêm class/enum/interface **rỗng** (chưa có business logic) cho domain Exchange — phần logic nghiệp vụ bạn tự viết tay sau.

---

```
Trong project Spring Boot hiện tại (đã có sẵn package domain/application/infrastructure/api cho Account),
hãy tạo thêm SKELETON cho domain Exchange, đặt trong package con `domain.exchange`
(ví dụ com.example.accountdemo.domain.exchange).

QUAN TRỌNG: Chỉ tạo SKELETON — enum thì tạo đủ giá trị (enum không cần TODO vì không có logic phức tạp),
còn class/method có business logic thì chỉ để signature, thân hàm chỉ chứa
`throw new UnsupportedOperationException("TODO: tự viết");` hoặc comment `// TODO: tự viết logic ở đây`.
KHÔNG được tự ý viết business logic bên trong các method của Order và OrderBook — tôi sẽ tự viết
phần đó để luyện tập, đây là mục đích chính của bài tập.

## Enum (trong domain.exchange)
- `OrderSide`: BUY, SELL
- `OrderType`: MARKET, LIMIT
- `OrderStatus`: PENDING, PARTIALLY_FILLED, FILLED, CANCELLED — thêm 1 method `isFinal()` trả về
  true nếu là FILLED hoặc CANCELLED (method này đơn giản, có thể viết logic thật luôn, không cần TODO).

## Value Object (trong domain.exchange, tất cả đều immutable — final class, final field)
- `Price` — field `long value`. Method rỗng: constructor, getValue(), isGreaterThan(Price other),
  isLessThan(Price other). Chỉ để signature + TODO, tôi tự viết validate và logic so sánh.
- `Quantity` — field `long value`. Method rỗng: constructor, getValue(), plus(Quantity other),
  minus(Quantity other), isGreaterThanOrEqual(Quantity other), isZero(). Chỉ để signature + TODO.
- `TradingPair` — field `String baseCurrency`, `String quoteCurrency`. Method rỗng: constructor,
  getBaseCurrency(), getQuoteCurrency(), toString() (có thể override thật, không cần TODO vì
  chỉ là format chuỗi, không phải business rule).

## Aggregate Root (trong domain.exchange)
- `Order` — field: orderId (String), accountId (String), side (OrderSide), orderType (OrderType),
  tradingPair (TradingPair), quantity (Quantity), price (Price, có thể null), filledQuantity (Quantity),
  status (OrderStatus).
  Method rỗng cần có: constructor, match(Quantity executedQuantity), cancel(), getRemainingQuantity(),
  và các getter (getOrderId, getAccountId, getSide, getOrderType, getTradingPair, getPrice, getStatus).
  TẤT CẢ method có logic (constructor, match, cancel, getRemainingQuantity) chỉ để TODO comment,
  không viết logic thật.
- `OrderBook` — field: tradingPair (TradingPair), buyOrders (List<Order>), sellOrders (List<Order>).
  Method rỗng cần có: constructor(TradingPair pair), addOrder(Order order), removeOrder(String orderId),
  getBestBid() trả về Optional<Price>, getBestAsk() trả về Optional<Price>.
  TẤT CẢ các method này chỉ để TODO, không viết logic thật (đặc biệt addOrder — đây là chỗ chứa
  business rule quan trọng nhất, tuyệt đối không tự ý implement).

## Repository interface (Port, trong domain.exchange)
- `OrderRepository`: `Order findById(String orderId)`, `void save(Order order)`.
- `OrderBookRepository`: `OrderBook findByTradingPair(TradingPair pair)`, `void save(OrderBook orderBook)`.

RÀNG BUỘC BẮT BUỘC: toàn bộ file trong domain.exchange TUYỆT ĐỐI KHÔNG được import bất kỳ thứ gì
từ javax.persistence, jakarta.persistence, org.springframework.* — giữ đúng nguyên tắc domain
không phụ thuộc framework/hạ tầng (giống Account ở Sprint 0).

## Test
Tạo sẵn 1 file test rỗng `OrderTest` và 1 file `OrderBookTest` (JUnit 5, KHÔNG dùng @SpringBootTest,
test thuần Java) trong thư mục test tương ứng, mỗi file có các method trống (chỉ tên method + TODO,
không viết assert):
- OrderTest: `cancel_shouldThrowWhenOrderAlreadyFilled()`, `match_shouldSetStatusFilledWhenFullyMatched()`,
  `match_shouldSetStatusPartiallyFilledWhenPartialMatch()`, `placeOrder_shouldThrowWhenLimitOrderHasNoPrice()`
- OrderBookTest: `addOrder_shouldKeepBestBidAsHighestBuyPrice()`, `addOrder_shouldKeepBestAskAsLowestSellPrice()`

## Sau khi tạo xong
Liệt kê lại cho tôi toàn bộ danh sách file đã tạo và đường dẫn, để tôi biết bắt đầu viết logic từ đâu.
```

---

## Lưu ý sau khi Cursor tạo xong

1. Viết logic theo đúng thứ tự: `OrderStatus.isFinal()` (nếu Cursor chưa viết sẵn) → `Price`/`Quantity`/`TradingPair` → `Order` → `OrderBook`. Đừng nhảy vào `OrderBook` trước vì nó không dùng được nếu `Order` chưa xong.
2. Nếu Cursor "lỡ tay" viết luôn logic bên trong `Order.cancel()`, `Order.match()`, hay `OrderBook.addOrder()` — xóa phần thân hàm đó và tự viết lại. Đây là phần cốt lõi của bài học.
3. Viết xong mỗi Aggregate, chuyển ngay sang viết unit test tương ứng trước khi làm tiếp — đừng viết hết code rồi mới test dồn một lượt, làm vậy khó phát hiện chỗ sai sớm.
