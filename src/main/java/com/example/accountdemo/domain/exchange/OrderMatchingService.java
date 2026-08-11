package com.example.accountdemo.domain.exchange;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        if (incomingOrder == null) {
            throw new IllegalArgumentException("incomingOrder không được null");
        }
        if (orderBook == null) {
            throw new IllegalArgumentException("orderBook không được null");
        }

        List<Trade> trades = new ArrayList<>();
        Set<Order> affected = new LinkedHashSet<>();
        affected.add(incomingOrder);

        // Lặp: còn khối lượng chưa khớp và còn đối ứng giá phù hợp thì khớp tiếp
        while (!incomingOrder.getRemainingQuantity().isZero()) {
            // BUY → lấy lệnh bán giá thấp nhất; SELL → lấy lệnh mua giá cao nhất
            Optional<Order> oppositeOpt = findBestOpposite(incomingOrder, orderBook);
            if (oppositeOpt.isEmpty()) {
                break; // sổ hết lệnh đối ứng đang mở
            }

            Order opposite = oppositeOpt.get();
            // LIMIT: giá mua phải >= giá bán; MARKET: luôn coi là chấp nhận giá đối ứng
            if (!isPriceCompatible(incomingOrder, opposite)) {
                break; // giá không chồng → dừng, phần dư sẽ vào sổ chờ
            }

            // Khớp số lượng nhỏ hơn giữa 2 bên (vd mua 10, bán 5 → khớp 5)
            Quantity matchQty = minQuantity(
                    incomingOrder.getRemainingQuantity(),
                    opposite.getRemainingQuantity()
            );
            // Giá giao dịch lấy theo lệnh đã nằm sẵn trên sổ (maker)
            Price matchPrice = opposite.getPrice();
            if (matchPrice == null) {
                throw new IllegalStateException("Lệnh trên sổ phải có giá để khớp: " + opposite.getOrderId());
            }

            // Cập nhật filledQuantity + status (PENDING → PARTIALLY_FILLED / FILLED)
            incomingOrder.match(matchQty);
            opposite.match(matchQty);
            affected.add(opposite);

            // Ghi nhận 1 lần khớp (ai mua, ai bán, bao nhiêu, giá nào)
            trades.add(createTrade(incomingOrder, opposite, matchQty, matchPrice));

            // Lệnh đối ứng khớp hết → gỡ khỏi sổ (không còn chờ)
            if (opposite.getStatus() == OrderStatus.FILLED) {
                orderBook.removeOrder(opposite.getOrderId());
            }
        }

        // Phần lệnh mới còn dư sau khi khớp
        if (!incomingOrder.getRemainingQuantity().isZero()) {
            if (incomingOrder.getOrderType() == OrderType.MARKET) {
                // MARKET không "chờ" trên sổ → hủy phần chưa khớp
                incomingOrder.cancel();
            } else {
                // LIMIT còn dư → đưa lên sổ chờ người khác khớp sau
                orderBook.addOrder(incomingOrder);
            }
        }

        return new MatchResult(trades, new ArrayList<>(affected));
    }

    /** Tìm lệnh đối ứng tốt nhất còn mở (chưa FILLED/CANCELLED). */
    private Optional<Order> findBestOpposite(Order incoming, OrderBook orderBook) {
        List<Order> oppositeSide = incoming.getSide() == OrderSide.BUY
                ? orderBook.getSellOrders()  // đã sort giá tăng dần → phần tử đầu = rẻ nhất
                : orderBook.getBuyOrders();  // đã sort giá giảm dần → phần tử đầu = cao nhất

        return oppositeSide.stream()
                .filter(order -> !order.getStatus().isFinal())
                .findFirst();
    }

    /**
     * Hai lệnh có "gặp nhau" về giá không?
     * LIMIT mua X, bán Y → khớp khi X >= Y (người mua chịu trả ít nhất bằng giá bán).
     */
    private boolean isPriceCompatible(Order incoming, Order opposite) {
        if (incoming.getOrderType() == OrderType.MARKET) {
            return true;
        }

        Price buyPrice;
        Price sellPrice;
        if (incoming.getSide() == OrderSide.BUY) {
            buyPrice = incoming.getPrice();
            sellPrice = opposite.getPrice();
        } else {
            buyPrice = opposite.getPrice();
            sellPrice = incoming.getPrice();
        }

        if (buyPrice == null || sellPrice == null) {
            return false;
        }
        return buyPrice.getValue() >= sellPrice.getValue();
    }

    private Quantity minQuantity(Quantity a, Quantity b) {
        return a.getValue() <= b.getValue() ? a : b;
    }

    private Trade createTrade(Order incoming, Order opposite, Quantity qty, Price price) {
        if (incoming.getSide() == OrderSide.BUY) {
            return new Trade(incoming.getOrderId(), opposite.getOrderId(), qty, price);
        }
        return new Trade(opposite.getOrderId(), incoming.getOrderId(), qty, price);
    }
}
