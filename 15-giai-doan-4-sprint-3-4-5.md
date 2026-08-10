# Giai đoạn 4 — Sprint 3, 4, 5: Đặt lệnh qua API, Khớp lệnh, Domain Event
## Hướng dẫn (bạn tự viết code, đây chỉ là khung gợi ý — giống cách làm Sprint 0-1)

*Điều kiện tiên quyết: đã xong Sprint 1 (`domain/`: enum, Value Object, `Order`, `OrderBook`, Repository interface) và Sprint 2 (`infrastructure/`: JPA adapter cho `OrderRepository`/`OrderBookRepository`, theo đúng mẫu `AccountRepositoryJpaImpl` bạn đã làm ở Account).*

### Nhắc lại mô hình đã thống nhất trong project của bạn

- **OrderBook** = sổ lệnh của **một cặp** (vd BTC/VND). **Admin/seed tạo sổ trước** (xem `SeedDataConfig` / `data.sql`) — user **không** tự tạo sổ khi đặt lệnh.
- **Order** = một lệnh mua/bán của user, được đưa vào sổ đã mở.
- Khi đặt lệnh: phải **tìm sổ theo cặp** → nếu chưa có sổ → **reject** (cặp chưa được mở), không auto-create.
- `OrderRepository.findById` / `OrderBookRepository.findByTradingPair` hiện trả về **`null`** khi không tìm thấy (giống `AccountRepository`) — giữ nhất quán kiểu này.

---

## Sprint 3 — Use case "Đặt lệnh" qua API

**Mục tiêu**: gọi API → lệnh mới được tạo và lưu vào `OrderBook` **đã tồn tại**. **Chưa khớp lệnh ở Sprint này** — khớp lệnh để dành cho Sprint 4, làm riêng cho dễ kiểm soát.

### 1. API layer — `api/`

```java
public record PlaceOrderRequest(
    String accountId,
    String side,         // "BUY" / "SELL" -> parse thành OrderSide
    String orderType,    // "MARKET" / "LIMIT" -> parse thành OrderType
    String baseCurrency,
    String quoteCurrency,
    long quantity,
    Long price           // nullable — null nếu là MARKET order
) {}
```

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderApplicationService placeOrderApplicationService;

    // constructor injection — giống AccountController

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
        // TODO:
        // 1. Parse string → enum/VO ở đây (api layer):
        //    OrderSide.valueOf(request.side()), OrderType.valueOf(request.orderType()),
        //    new TradingPair(...), new Quantity(...), price == null ? null : new Price(...)
        // 2. Gọi placeOrderApplicationService.placeOrder(...)
        // 3. Trả về orderId + status
        // Gợi ý response: record PlaceOrderResponse(String orderId, String status)
    }
}
```

*Quy ước của project*: parse `"BUY"` → `OrderSide.BUY` **ở tầng `api/`**, để `application/` chỉ nhận type đã sạch — tránh String tràn xuống domain.

### 2. Application layer — `application/`

```java
@Service
public class PlaceOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderBookRepository orderBookRepository;
    // (tuỳ chọn) AccountRepository — nếu muốn check account tồn tại trước khi đặt lệnh

    // constructor injection

    public String placeOrder(
            String accountId, OrderSide side, OrderType orderType,
            TradingPair tradingPair, Quantity quantity, Price price
    ) {
        // TODO: các bước điều phối (KHÔNG viết if business rule ở đây — logic đã nằm trong Order/OrderBook rồi):
        // 1. (Tuỳ chọn) accountRepository.findById(accountId) — null thì throw, giống DepositApplicationService
        // 2. Tạo orderId mới (gợi ý: UUID.randomUUID().toString())
        // 3. new Order(orderId, accountId, side, orderType, tradingPair, quantity, price)
        //    -> constructor của Order sẽ tự validate (LIMIT phải có price, ...)
        // 4. OrderBook orderBook = orderBookRepository.findByTradingPair(tradingPair)
        //    -> Nếu null: throw IllegalArgumentException("Cặp giao dịch chưa được mở: " + tradingPair)
        //       (KHÔNG tạo OrderBook mới ở đây — sổ do admin/seed mở sẵn)
        // 5. orderBook.addOrder(order)
        //    -> OrderBook sẽ tự reject nếu order sai cặp (isSameTradingPair)
        // 6. orderRepository.save(order)
        // 7. orderBookRepository.save(orderBook)
        // 8. return order.getOrderId()
    }
}
```

*Lưu ý về repository*: project đang dùng `null` khi không tìm thấy (giống Account). Giữ nhất quán — check `null` rồi throw, **không** `orElseGet(() -> new OrderBook(...))`.

### 3. Test gợi ý cho Sprint 3

- `placeOrder_shouldSaveOrderIntoOrderBook()` — dùng **fake repository** (implement `OrderRepository`/`OrderBookRepository` bằng `HashMap` trong bộ nhớ). Fake OrderBook repo phải **có sẵn** sổ BTC/VND trước khi gọi placeOrder (giống admin đã mở cặp).
- `placeOrder_shouldRejectWhenOrderBookNotOpened()` — chưa seed sổ → throw.
- `placeOrder_shouldRejectLimitOrderWithoutPrice()` — rule đã nằm trong `Order` constructor (Sprint 1); test ở đây chỉ xác nhận exception "xuyên" lên application, không bị nuốt mất.

---

## Sprint 4 — Domain Service: Khớp lệnh (phần khó nhất)

**Vì sao cần Domain Service riêng, không nhét logic này vào `Order` hay `OrderBook`?**

Vì khớp lệnh là hành vi **liên quan đến nhiều Order khác nhau cùng lúc** (1 lệnh mới đến, so khớp với nhiều lệnh đang chờ trong `OrderBook`) — không thuộc về riêng 1 Aggregate nào. Đây là trường hợp kinh điển để dùng **Domain Service**: logic nghiệp vụ thật, nhưng không có "chủ nhân" tự nhiên là 1 Aggregate cụ thể.

### 1. Kết quả khớp lệnh — nên có 1 kiểu dữ liệu riêng

```java
// domain/exchange/Trade.java (Value Object, immutable — ghi lại 1 lần khớp thành công)
public final class Trade {
    private final String buyOrderId;
    private final String sellOrderId;
    private final Quantity matchedQuantity;
    private final Price matchedPrice;

    // TODO: constructor, getter — không cần logic phức tạp, chỉ lưu dữ liệu
}
```

*Vì sao cần `Trade` riêng?* Vì Sprint 5 (domain event) sẽ cần đúng những thông tin này để publish event — tách sẵn từ Sprint 4 để Sprint 5 nhàn hơn.

### 2. Domain Service (class thuần Java trong `domain/` — KHÔNG gắn `@Service` Spring)

```java
// domain/exchange/OrderMatchingService.java
public class OrderMatchingService {

    public List<Trade> match(Order incomingOrder, OrderBook orderBook) {
        // TODO: đây là phần bạn tự thiết kế thuật toán — vài câu hỏi định hướng:
        //
        // - Nếu incomingOrder là BUY: so khớp với sellOrders, ưu tiên giá THẤP NHẤT (bestAsk).
        // - Nếu incomingOrder là SELL: so khớp với buyOrders, ưu tiên giá CAO NHẤT (bestBid).
        // - Vòng lặp dừng khi: incomingOrder.getRemainingQuantity().isZero()
        //   HOẶC không còn lệnh đối ứng nào có giá phù hợp.
        // - LIMIT: giá mua >= giá bán thì khớp được.
        // - MARKET: không quan tâm giá, khớp nếu còn lệnh đối ứng.
        // - Khối lượng khớp = min(remaining của 2 bên)
        //   -> incomingOrder.match(...) và oppositeOrder.match(...)
        //   -> tạo Trade, thêm vào list kết quả
        // - oppositeOrder FILLED -> orderBook.removeOrder(...)
        // - incomingOrder LIMIT còn dư -> orderBook.addOrder(incomingOrder) để chờ
        // - incomingOrder MARKET còn dư -> chọn 1:
        //   (a) cancel phần dư (market không "chờ"), hoặc
        //   (b) throw "không đủ thanh khoản"

        return null; // TODO
    }
}
```

**Cách wire Domain Service (quan trọng — đừng gắn Spring vào `domain/`):**

```java
// infrastructure/config/ExchangeDomainConfig.java (hoặc tương tự)
@Configuration
public class ExchangeDomainConfig {

    @Bean
    public OrderMatchingService orderMatchingService() {
        return new OrderMatchingService(); // domain class thuần, Spring chỉ tạo bean giúp inject
    }
}
```

Rồi `PlaceOrderApplicationService` nhận `OrderMatchingService` qua constructor — giống inject repository.

*Câu hỏi mở*: nếu 2 lệnh SELL cùng giá đang chờ, lệnh nào khớp trước? Sàn thật dùng **price-time priority**. `OrderBook` của bạn hiện sort theo giá — đã đủ cho price priority; time priority cần thêm thời điểm đặt lệnh nếu muốn làm chặt hơn (tuỳ chọn).

### 3. Nối vào Application Service (sửa lại Sprint 3)

```java
public String placeOrder(...) {
    // 1–4: tạo Order + load OrderBook (reject nếu null) — như Sprint 3
    // 5. Thay vì chỉ orderBook.addOrder(order), giờ gọi:
    List<Trade> trades = orderMatchingService.match(order, orderBook);

    // 6. LƯU Ý QUAN TRỌNG — dễ quên và gây bug:
    //    Sau match, không chỉ incoming order đổi trạng thái.
    //    Các lệnh đối ứng đã match() cũng đã thay đổi (PARTIALLY_FILLED / FILLED).
    //    Phải save TẤT CẢ order bị đụng tới:
    //    - orderRepository.save(incomingOrder)
    //    - với mỗi Trade: save buy order + sell order (load theo id từ Trade, hoặc
    //      để OrderMatchingService / Trade mang thêm reference, hoặc
    //      orderBookRepository.save(orderBook) nếu adapter của bạn đã save mọi order trong sổ)
    //    Kiểm tra lại OrderBookRepositoryJpaImpl.save(...) của bạn: nó đã lưu buyOrders + sellOrders chưa?
    //    Nếu rồi thì save orderBook có thể đủ; nếu chưa — phải save từng order đối ứng tường minh.
}
```

### 4. Test gợi ý cho Sprint 4 (phần đáng viết test nhất)

- `match_shouldFullyMatchWhenPriceCrosses()` — 1 buy, 1 sell, giá chồng, khối lượng bằng → cả 2 FILLED.
- `match_shouldPartiallyMatchWhenQuantityDiffers()` — mua nhiều / bán ít → 1 FILLED, 1 PARTIALLY_FILLED.
- `match_shouldNotMatchWhenPriceDoesNotCross()` — giá mua < giá bán → không khớp, cả 2 PENDING trong sổ.
- `match_shouldMatchAgainstBestPriceFirst()` — nhiều lệnh đối ứng → giá tốt nhất khớp trước.
- `match_shouldMatchMultipleOrdersWhenOneLargeOrderComesIn()` — 1 lệnh lớn khớp 2–3 lệnh nhỏ liên tiếp.
- `match_shouldRejectOrLeaveUnmatchedWhenOrderBookEmpty()` — sổ mở nhưng chưa có lệnh đối ứng.

---

## Sprint 5 — Domain Event khi khớp lệnh thành công

**Phạm vi Sprint 5 (đọc kỹ để khớp kế hoạch học):**

| Làm trong Sprint 5 | Chưa làm (để Giai đoạn 5 / nâng cao) |
|--------------------|--------------------------------------|
| Domain Event dạng **thông báo** (`TradeExecutedEvent`) | Event Sourcing |
| Port `DomainEventPublisher` + adapter **log ra console** | Outbox Pattern đầy đủ |
| Publish từ Application Service sau khi match | Kafka / message broker thật |

Kế hoạch gốc có dòng “Domain Event + Outbox Pattern”. Ở Sprint này bạn chỉ làm **nửa đầu (Domain Event notification)**. Outbox thật = bài toán “publish trước mà save fail / save rồi mà publish fail” — biết vấn đề tồn tại là đủ; implement Outbox để bước sau.

Đây **không phải Event Sourcing**: state vẫn lưu qua JPA như bình thường; event chỉ báo “vừa khớp xong một trade”.

### 1. Domain Event

```java
// domain/exchange/event/TradeExecutedEvent.java
public final class TradeExecutedEvent {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final TradingPair tradingPair;
    private final Quantity quantity;
    private final Price price;
    private final Instant occurredAt;

    // TODO: constructor, getter — chỉ lưu dữ liệu, không có logic
}
```

### 2. Port để publish event (Hexagonal — domain không biết publish bằng công nghệ gì)

```java
// domain/exchange/DomainEventPublisher.java
public interface DomainEventPublisher {
    void publish(TradeExecutedEvent event);
}
```

```java
// infrastructure/event/LoggingDomainEventPublisher.java
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {
    // TODO: chỉ log ra console (VD: log.info("Trade executed: {}", event))
    // Sau này Kafka/Outbox = thêm 1 implementation khác của cùng interface,
    // không đụng domain/application.
}
```

### 3. Nối vào Application Service

```java
public String placeOrder(...) {
    // ... (Sprint 3 + 4)
    List<Trade> trades = orderMatchingService.match(order, orderBook);
    // TODO: save repositories trước (incoming + đối ứng + orderBook)
    // TODO: với mỗi Trade → tạo TradeExecutedEvent → domainEventPublisher.publish(event)
    //
    // Câu hỏi: publish TRƯỚC hay SAU khi save?
    // - Publish trước, save fail → hệ thống khác nghĩ đã khớp nhưng DB không có → sai.
    // - Save trước, publish fail → DB đúng nhưng không ai nhận được event → cũng sai.
    // Production giải bằng Outbox Pattern (chưa implement ở Sprint này).
}
```

### 4. Test gợi ý cho Sprint 5

- `placeOrder_shouldPublishEventWhenTradeExecuted()` — fake `DomainEventPublisher` (lưu event vào `List` trong memory), assert số lần publish + dữ liệu.
- `placeOrder_shouldNotPublishEventWhenNoMatch()` — đặt lệnh không khớp ai → không publish event nào.

---

## Việc cần làm tiếp theo

Làm lần lượt **Sprint 3 → 4 → 5**, viết test ngay sau mỗi phần (đặc biệt Sprint 4).

Checklist nhanh trước khi sang bước nâng cao:

- [ ] Sprint 3: API đặt lệnh, reject khi sổ chưa mở, lưu order vào sổ đã seed
- [ ] Sprint 4: `OrderMatchingService` + `Trade`, save đủ lệnh đối ứng
- [ ] Sprint 5: `TradeExecutedEvent` + logging publisher (chưa Outbox)

Sau khi xong cả 3, review code rồi mới quyết định làm thêm Outbox thật / Kafka / Axon (Giai đoạn 5).
