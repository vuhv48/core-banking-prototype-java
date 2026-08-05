# Giai đoạn 4 — Sprint 1: Mô hình hóa domain Exchange
## Hướng dẫn (bạn tự viết code, đây chỉ là khung gợi ý — giống cách làm Account)

*Chỉ viết `domain/`, chưa động tới DB/API. Dùng lại Ubiquitous Language + business rule đã ghi chú ở Giai đoạn 1-2.*

---

## Nhắc lại nhanh những gì đã học (để bạn tự đối chiếu khi viết)

Từ Giai đoạn 2 (đọc `Order.php`/`OrderBook.php` thật):
- Có **2 Aggregate Root**: `Order` (vòng đời 1 lệnh) và `OrderBook` (tập hợp lệnh mở theo cặp tài sản).
- `Order` bảo vệ rule: không hủy lệnh đã ở trạng thái cuối (Filled/Cancelled).
- `OrderBook` bảo vệ rule: luôn giữ đúng thứ tự giá để bestBid/bestAsk chính xác.
- Điểm cần làm **chặt hơn** bản gốc: bản FinAegis không validate `amount`/`price` ngay trong `placeOrder()` — bạn nên tự làm chặt phần này khi viết Java.

---

## Danh sách file cần viết (chỉ `domain/`)

### 1. Enum

- **`OrderSide`**: `BUY`, `SELL`
- **`OrderType`**: `MARKET`, `LIMIT`
- **`OrderStatus`**: `PENDING`, `PARTIALLY_FILLED`, `FILLED`, `CANCELLED` — gợi ý thêm 1 method `isFinal()` ngay trong enum (giống bản FinAegis đã đọc), trả về `true` nếu là `FILLED` hoặc `CANCELLED`.

### 2. Value Object

**`Price`** (tương tự cách bạn đã làm `Money` ở Account — immutable, validate):
```java
public final class Price {
    private final long value; // hoặc dùng BigDecimal nếu muốn chính xác cao hơn long

    // constructor: validate value > 0 (KHÔNG cho giá âm hoặc = 0)
    // getValue()
    // isGreaterThan(Price other), isLessThan(Price other)
}
```

**`Quantity`** (tương tự `Price`):
```java
public final class Quantity {
    private final long value;

    // constructor: validate value > 0
    // getValue()
    // plus(Quantity other), minus(Quantity other) -> trả về Quantity mới (immutable)
    // isGreaterThanOrEqual(Quantity other), isZero()
}
```

**`TradingPair`** (cặp tài sản, ví dụ BTC/USD):
```java
public final class TradingPair {
    private final String baseCurrency;
    private final String quoteCurrency;

    // constructor: validate cả 2 không null/rỗng, và baseCurrency != quoteCurrency
    // getBaseCurrency(), getQuoteCurrency()
    // toString() -> nên trả về dạng "BTC/USD" (tiện cho log/generateId)
}
```

### 3. Aggregate Root — `Order`

```java
public class Order {
    private String orderId;
    private String accountId;
    private OrderSide side;
    private OrderType orderType;
    private TradingPair tradingPair;
    private Quantity quantity;
    private Price price;           // null nếu là MARKET order
    private Quantity filledQuantity;
    private OrderStatus status;

    // constructor (đóng vai trò "placeOrder"):
    // TODO: bạn tự viết — cần bảo vệ những rule nào? Gợi ý:
    //   - quantity phải > 0 (Quantity đã tự validate ở constructor của nó rồi,
    //     nhưng Order nên kiểm tra thêm: nếu orderType == LIMIT thì price KHÔNG được null)
    //   - nếu orderType == MARKET thì price PHẢI null (market order không có giá cố định)
    //   - status khởi tạo = PENDING, filledQuantity khởi tạo = 0

    public void match(Quantity executedQuantity) {
        // TODO: bạn tự viết
        // - cộng dồn filledQuantity
        // - nếu filledQuantity == quantity -> status = FILLED
        // - còn lại -> status = PARTIALLY_FILLED
        // (đây chính là rule bạn đã thấy trong applyOrderMatched() ở Giai đoạn 2)
    }

    public void cancel() {
        // TODO: bạn tự viết — RULE QUAN TRỌNG NHẤT:
        // nếu status.isFinal() == true -> throw exception, không cho hủy
        // ngược lại -> status = CANCELLED
    }

    public Quantity getRemainingQuantity() {
        // TODO: quantity - filledQuantity
    }

    // getter còn lại: getOrderId(), getAccountId(), getSide(), getOrderType(),
    // getTradingPair(), getPrice(), getStatus()
}
```

### 4. Aggregate Root — `OrderBook`

```java
public class OrderBook {
    private TradingPair tradingPair;
    private List<Order> buyOrders;   // hoặc dùng cấu trúc dữ liệu khác nếu bạn muốn
    private List<Order> sellOrders;

    // constructor: khởi tạo 2 danh sách rỗng theo 1 TradingPair cụ thể

    public void addOrder(Order order) {
        // TODO: bạn tự viết — RULE QUAN TRỌNG NHẤT:
        // thêm order vào đúng danh sách (buy/sell theo order.getSide())
        // rồi SẮP XẾP LẠI: buyOrders theo giá GIẢM DẦN (giá cao nhất lên đầu),
        // sellOrders theo giá TĂNG DẦN (giá thấp nhất lên đầu)
        // (đây chính là bestBid/bestAsk logic đã học ở Giai đoạn 2)
    }

    public void removeOrder(String orderId) {
        // TODO: xóa order khỏi cả 2 danh sách (dựa theo orderId), rồi có cần
        // tính lại gì không? (gợi ý: nếu bestBid/bestAsk là field riêng thì cần)
    }

    public Optional<Price> getBestBid() {
        // TODO: giá cao nhất trong buyOrders (phần tử đầu tiên sau khi đã sort)
    }

    public Optional<Price> getBestAsk() {
        // TODO: giá thấp nhất trong sellOrders
    }
}
```

*(Gợi ý thiết kế thêm, tùy bạn quyết định: có nên tách riêng field `bestBid`/`bestAsk` được cập nhật mỗi lần add/remove — giống bản FinAegis — hay tính lại on-the-fly mỗi lần gọi `getBestBid()`? Cả 2 cách đều hợp lý, cách đầu nhanh hơn khi đọc nhiều, cách sau đơn giản hơn khi code. Đây là quyết định thiết kế của riêng bạn.)*

### 5. Repository (Port, giống `AccountRepository` đã làm)

```java
public interface OrderRepository {
    Order findById(String orderId);
    void save(Order order);
}

public interface OrderBookRepository {
    OrderBook findByTradingPair(TradingPair pair);
    void save(OrderBook orderBook);
}
```

---

## Unit test cần viết (chưa cần Spring/DB)

Gợi ý các case quan trọng nhất (theo đúng khuôn Arrange–Act–Assert bạn đã quen ở `AccountTest`):

- `cancel_shouldThrowWhenOrderAlreadyFilled()` — rule quan trọng nhất của `Order`
- `match_shouldSetStatusFilledWhenFullyMatched()`
- `match_shouldSetStatusPartiallyFilledWhenPartialMatch()`
- `placeOrder_shouldThrowWhenLimitOrderHasNoPrice()`
- `addOrder_shouldKeepBestBidAsHighestBuyPrice()` — rule quan trọng nhất của `OrderBook`
- `addOrder_shouldKeepBestAskAsLowestSellPrice()`

---

## Việc cần làm tiếp theo

Viết xong domain/ (enum + Value Object + 2 Aggregate + Repository interface) và ít nhất vài unit test ở trên, gửi lại (hoặc trỏ đường dẫn project như lần trước) để review trước khi sang Sprint 2 (Repository adapter JPA).
