package com.example.accountdemo.domain.exchange.orderbook.model;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookTest {

    private static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(BTC_VND);
    }

    private Order buyOrder(String id, long price) {
        return new Order(
                id,
                "ACC-001",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(10),
                new Price(price)
        );
    }

    private Order sellOrder(String id, long price) {
        return new Order(
                id,
                "ACC-002",
                OrderSide.SELL,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(10),
                new Price(price)
        );
    }

    @Test
    void addOrder_shouldKeepBestBidAsHighestBuyPrice() {
        orderBook.addOrder(buyOrder("BUY-1", 58_000_000));
        orderBook.addOrder(buyOrder("BUY-2", 60_000_000));
        orderBook.addOrder(buyOrder("BUY-3", 59_000_000));

        assertTrue(orderBook.getBestBid().isPresent());
        assertEquals(0, BigDecimal.valueOf(60_000_000).compareTo(orderBook.getBestBid().get().getValue()));
    }

    @Test
    void addOrder_shouldKeepBestAskAsLowestSellPrice() {
        orderBook.addOrder(sellOrder("SELL-1", 62_000_000));
        orderBook.addOrder(sellOrder("SELL-2", 61_000_000));
        orderBook.addOrder(sellOrder("SELL-3", 63_000_000));

        assertTrue(orderBook.getBestAsk().isPresent());
        assertEquals(0, BigDecimal.valueOf(61_000_000).compareTo(orderBook.getBestAsk().get().getValue()));
    }
}
