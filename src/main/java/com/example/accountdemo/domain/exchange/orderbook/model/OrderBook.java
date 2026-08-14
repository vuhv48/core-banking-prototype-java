package com.example.accountdemo.domain.exchange.orderbook.model;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Aggregate Root — một bản ghi sổ lệnh (một cặp).
 *
 * <pre>
 * tradingPair = BTC/VND
 * buyOrders   = [ORD-BUY-001  BUY  LIMIT  qty=1  price=60_000_000  PENDING]
 * sellOrders  = [ORD-SELL-001 SELL LIMIT  qty=2  price=61_000_000  PENDING]
 * </pre>
 *
 * Không trừ tiền. Khớp do OrderMatchingService. getBuy/SellOrders() trả copy.
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

    public OrderBook(TradingPair pair) {
        if (pair == null) {
            throw new IllegalArgumentException("tradingPair không được null");
        }
        this.tradingPair = pair;
        this.buyOrders = new ArrayList<>();
        this.sellOrders = new ArrayList<>();
    }

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
                    (Order o) -> o.getPrice() != null ? o.getPrice().getValue() : Long.MIN_VALUE
            ).reversed());
        } else {
            sellOrders.add(order);
            sellOrders.sort(Comparator.comparing(
                    (Order o) -> o.getPrice() != null ? o.getPrice().getValue() : Long.MAX_VALUE
            ));
        }
    }

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

    public Optional<Price> getBestBid() {
        return buyOrders.stream()
                .map(Order::getPrice)
                .filter(price -> price != null)
                .max(Comparator.comparingLong(Price::getValue));
    }

    public Optional<Price> getBestAsk() {
        return sellOrders.stream()
                .map(Order::getPrice)
                .filter(price -> price != null)
                .min(Comparator.comparingLong(Price::getValue));
    }

    public List<Order> getBuyOrders() {
        return List.copyOf(buyOrders);
    }

    public List<Order> getSellOrders() {
        return List.copyOf(sellOrders);
    }

    private boolean isSameTradingPair(TradingPair other) {
        return tradingPair.getBaseCurrency().equalsIgnoreCase(other.getBaseCurrency())
                && tradingPair.getQuoteCurrency().equalsIgnoreCase(other.getQuoteCurrency());
    }
}
