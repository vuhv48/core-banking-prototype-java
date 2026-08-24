package com.example.accountdemo.domain.exchange.orderbook.model;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Aggregate Root — sổ lệnh của một cặp giao dịch.
 *
 * <p><b>Vì sao cần class này:</b> giữ danh sách bid/ask đã sort để matching tìm đối ứng
 * tốt nhất. Không trừ tiền — chỉ xếp hàng / gỡ lệnh; khớp do {@code OrderMatchingService}.
 *
 * <pre>
 * tradingPair = BTC/VND
 * buyOrders   = [ORD-BUY-001  BUY  LIMIT  qty=1  price=60_000_000  PENDING]
 * sellOrders  = [ORD-SELL-001 SELL LIMIT  qty=2  price=61_000_000  PENDING]
 * </pre>
 *
 * {@code getBuy/SellOrders()} trả copy — ngoài không sửa list nội bộ.
 */
@Getter
public class OrderBook {

    /** Cặp giao dịch của sổ này, vd BTC/VND. */
    private TradingPair tradingPair;
    /** Lệnh mua đang chờ (bid) — sort giá cao → thấp. */
    @Getter(AccessLevel.NONE)
    private List<Order> buyOrders;
    /** Lệnh bán đang chờ (ask) — sort giá thấp → cao. */
    @Getter(AccessLevel.NONE)
    private List<Order> sellOrders;

    /** Tạo sổ rỗng cho một cặp — chưa có lệnh chờ. */
    public OrderBook(TradingPair pair) {
        if (pair == null) {
            throw new IllegalArgumentException("tradingPair không được null");
        }
        this.tradingPair = pair;
        this.buyOrders = new ArrayList<>();
        this.sellOrders = new ArrayList<>();
    }

    /**
     * Đưa lệnh vào đúng phía (BUY → bid, SELL → ask) và sort giá.
     * Nghĩa nghiệp vụ: lệnh đang xếp hàng chờ đối ứng.
     */
    public void addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order không được null");
        }
        if (!isSameTradingPair(order.getTradingPair())) {
            throw new IllegalArgumentException(
                    "Cặp giao dịch không khớp: " + order.getTradingPair() + " vs " + tradingPair
            );
        }

        if (order.getSide() == OrderSide.BUY) {
            buyOrders.add(order);
            buyOrders.sort(Comparator.comparing(
                    (Order o) -> o.getPrice() != null ? o.getPrice().getValue() : BigDecimal.valueOf(Long.MIN_VALUE)
            ).reversed());
        } else {
            sellOrders.add(order);
            sellOrders.sort(Comparator.comparing(
                    (Order o) -> o.getPrice() != null ? o.getPrice().getValue() : BigDecimal.valueOf(Long.MAX_VALUE)
            ));
        }
    }

    /** Gỡ lệnh khỏi sổ (đã FILLED hoặc cancel) — tránh khớp lại vòng sau. */
    public void removeOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId không được null hoặc rỗng");
        }
        boolean removed = buyOrders.removeIf(order -> orderId.equals(order.getOrderId()))
                || sellOrders.removeIf(order -> orderId.equals(order.getOrderId()));
        if (!removed) {
            throw new IllegalArgumentException("Không tìm thấy lệnh: " + orderId);
        }
    }

    /** Giá mua tốt nhất (cao nhất) trên sổ — dùng tham khảo / UI. */
    public Optional<Price> getBestBid() {
        return buyOrders.stream()
                .map(Order::getPrice)
                .filter(price -> price != null)
                .max(Comparator.comparing(Price::getValue));
    }

    /** Giá bán tốt nhất (thấp nhất) trên sổ — dùng tham khảo / UI. */
    public Optional<Price> getBestAsk() {
        return sellOrders.stream()
                .map(Order::getPrice)
                .filter(price -> price != null)
                .min(Comparator.comparing(Price::getValue));
    }

    /** Copy list lệnh mua đang chờ — ngoài không phá sort nội bộ. */
    public List<Order> getBuyOrders() {
        return List.copyOf(buyOrders);
    }

    /** Copy list lệnh bán đang chờ — ngoài không phá sort nội bộ. */
    public List<Order> getSellOrders() {
        return List.copyOf(sellOrders);
    }

    private boolean isSameTradingPair(TradingPair other) {
        return tradingPair.getBaseCurrency().equalsIgnoreCase(other.getBaseCurrency())
                && tradingPair.getQuoteCurrency().equalsIgnoreCase(other.getQuoteCurrency());
    }
}
