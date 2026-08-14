package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderStatus;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import java.time.LocalDateTime;

/**
 * Dữ liệu mẫu dùng chung cho test Exchange (domain + persistence).
 */
public final class ExchangeTestData {

    public static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    private ExchangeTestData() {
    }

    public static Order limitBuyOrder() {
        return new Order(
                "ORD-BUY-001",
                "ACC-001",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(100),
                new Price(60_000_000)
        );
    }

    public static Order limitSellOrder() {
        return new Order(
                "ORD-SELL-001",
                "ACC-002",
                OrderSide.SELL,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(50),
                new Price(61_000_000)
        );
    }

    public static OrderBook sampleOrderBook() {
        OrderBook orderBook = new OrderBook(BTC_VND);
        orderBook.addOrder(limitBuyOrder());
        orderBook.addOrder(limitSellOrder());
        return orderBook;
    }

    public static OrderJpaEntity orderJpaEntity(String id, String side, long price, long quantity) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(id);
        entity.setAccountId("ACC-001");
        entity.setSide(side);
        entity.setOrderType(OrderType.LIMIT.name());
        entity.setBaseCurrency("BTC");
        entity.setQuoteCurrency("VND");
        entity.setQuantity(quantity);
        entity.setPrice(price);
        entity.setFilledQuantity(0);
        entity.setStatus(OrderStatus.PENDING.name());
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public static OrderBookJpaEntity orderBookJpaEntity() {
        OrderBookJpaEntity entity = new OrderBookJpaEntity();
        entity.setId("BTC/VND");
        entity.setBaseCurrency("BTC");
        entity.setQuoteCurrency("VND");
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
