package com.example.accountdemo.domain.exchange.matching;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderStatus;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;

/**
 * Domain Service (stateless) — nghiệp vụ khớp lệnh xuyên nhiều Aggregate.
 *
 * <p>Phân loại DDD:
 * <ul>
 *   <li>Không phải Aggregate — không có id, không lưu DB, không giữ state</li>
 *   <li>Logic không thuộc riêng {@link Order} hay {@link OrderBook} → tách thành service</li>
 * </ul>
 *
 * <p>Ví dụ: Chị B đã đặt bán 5 @ 60M trên sổ.
 * Anh A đặt mua 10 @ 60M → khớp 5; B FILLED; A còn 5 chờ trên sổ (PARTIALLY_FILLED).
 * Trả về {@link MatchResult} cho Application layer biết cần save / publish event gì.
 */
public class OrderMatchingService {

    /**
     * Thử khớp lệnh mới với các lệnh đang chờ trên sổ.
     *
     * @param incomingOrder lệnh vừa đặt (chưa nằm trên sổ)
     * @param orderBook     sổ đã load từ DB (có list mua/bán đang chờ)
     * @return danh sách Trade đã khớp + mọi Order bị đổi status (để Application save)
     */
    public MatchResult match(Order incomingOrder, OrderBook orderBook) {
        // ── LƯU Ý: class này CHỈ xử lý trên RAM (list Java), KHÔNG gọi DB ──
        // DB được lưu SAU ở PlaceOrderApplicationService:
        //   matchResult.getAffectedOrders() → orderRepository.save(...)
        //   orderBook (list buy/sell)       → orderBookRepository.save(...)

        if (incomingOrder == null) {
            throw new IllegalArgumentException("incomingOrder is null");
        }
        if (orderBook == null) {
            throw new IllegalArgumentException("orderBook is null");
        }

        // Danh sách Trade (VO) — Application dùng để publish event, không lưu bảng trades
        List<Trade> trades = new ArrayList<>();
        // Order nào bị đổi filled/status → Application sẽ orderRepository.save từng cái
        Set<Order> affected = new LinkedHashSet<>();
        affected.add(incomingOrder); // lệnh mới luôn bị đụng (dù khớp 0 hay khớp một phần)

        while (!incomingOrder.getRemainingQuantity().isZero()) {
            Optional<Order> oppositeOpt = findBestOpposite(incomingOrder, orderBook);
            if (oppositeOpt.isEmpty()) {
                break; // không còn ai đối diện trên sổ → thoát while
            }
            Order opposite = oppositeOpt.get();

            if (!isPriceCompatible(incomingOrder, opposite)) {
                break; // LIMIT: giá chưa gặp (vd mua 60M, bán 61M) → thoát while
            }

            // Khớp số nhỏ hơn giữa remaining hai bên (vd mua 10, bán 5 → khớp 5)
            Quantity matchQty = incomingOrder.getRemainingQuantity().getValue()
                    <= opposite.getRemainingQuantity().getValue()
                    ? incomingOrder.getRemainingQuantity()
                    : opposite.getRemainingQuantity();

            Price matchPrice = opposite.getPrice();
            if (matchPrice == null) {
                throw new IllegalStateException("Lệnh trên sổ phải có giá để khớp: " + opposite.getOrderId());
            }

            incomingOrder.match(matchQty); // tăng filledQuantity, đổi status trên object RAM
            opposite.match(matchQty);
            affected.add(opposite); // đối ứng cũng đổi → cần save DB sau (kể cả FILLED)

            trades.add(createTrade(incomingOrder, opposite, matchQty, matchPrice));

            if (opposite.getStatus() == OrderStatus.FILLED) {
                // Khớp hết → gỡ khỏi list buyOrders/sellOrders (RAM), tránh khớp lại vòng sau
                // Dòng FILLED vẫn nằm trong affected → vẫn save xuống bảng orders
                orderBook.removeOrder(opposite.getOrderId());
            }
        }

        // ── Sau while: xử lý phần lệnh MỚI còn chưa khớp hết ──
        if (!incomingOrder.getRemainingQuantity().isZero()) {
            if (incomingOrder.getOrderType() == OrderType.MARKET) {
                incomingOrder.cancel(); // MARKET: không treo sổ, hủy phần dư
            } else {
                // addOrder = đưa lệnh vào buyOrders HOẶC sellOrders (collection trên sổ)
                //
                // KHÔNG phải lưu DB trực tiếp!
                // Nghĩa nghiệp vụ: "lệnh này đang XẾP HÀNG chờ người khác khớp".
                //
                // Ví dụ: mua 10, mới khớp 8, còn 2:
                //   → add vào buyOrders → lần sau có người BÁN vào, findBestOpposite sẽ thấy lệnh này
                //
                // incomingOrder đã có trong affected (dòng 49) → save DB sẽ ghi filled=8, status=PARTIALLY_FILLED
                // PlaceOrderApplicationService còn gọi orderBookRepository.save → sync list sổ xuống DB
                orderBook.addOrder(incomingOrder);
            }
        }

        // Trả kết quả cho Application — đây là cầu nối sang bước LƯU DB + publish event
        return new MatchResult(trades, new ArrayList<>(affected));
    }

    private Optional<Order> findBestOpposite(Order incomingOrder, OrderBook orderBook) {
        List<Order> oppositeSide = incomingOrder.getSide() == OrderSide.BUY
                ? orderBook.getSellOrders()
                : orderBook.getBuyOrders();
        return oppositeSide.stream()
                .filter(order -> !order.getStatus().isFinal())
                .findFirst();
    }

    /**
     * Hai lệnh giá có "gặp nhau" để khớp được không?
     * Rule LIMIT: giá mua phải >= giá bán (người mua chịu trả ít nhất bằng giá người bán hỏi).
     * MARKET: không có giá riêng → luôn OK.
     */
    private boolean isPriceCompatible(Order incoming, Order opposite) {
        if (incoming.getOrderType() == OrderType.MARKET) {
            return true;
        }

        Price buyPrice;
        Price sellPrice;

        // Cần biết: trong cặp (incoming, opposite), lệnh nào là BUY, lệnh nào là SELL.
        // Cách dễ: nhìn theo incoming (lệnh mới), không nhìn opposite.
        if (incoming.getSide() == OrderSide.BUY) {
            // Lệnh mới là MUA → đối ứng trên sổ là BÁN
            buyPrice = incoming.getPrice();   // giá mua = giá lệnh mới
            sellPrice = opposite.getPrice();  // giá bán = giá lệnh trên sổ
        } else {
            // Lệnh mới là BÁN → đối ứng trên sổ là MUA
            buyPrice = opposite.getPrice();   // giá mua = giá lệnh trên sổ
            sellPrice = incoming.getPrice();  // giá bán = giá lệnh mới
        }

        if (buyPrice == null || sellPrice == null) {
            return false;
        }
        // Gặp nhau khi người mua chịu trả >= giá người bán hỏi
        return buyPrice.getValue() >= sellPrice.getValue();
    }

    private Trade createTrade(Order incoming, Order opposite, Quantity qty, Price price) {
        if (incoming.getSide() == OrderSide.BUY) {
            return new Trade(incoming.getOrderId(), opposite.getOrderId(), qty, price);
        }
        return new Trade(opposite.getOrderId(), incoming.getOrderId(), qty, price);
    }
}
