package com.example.accountdemo.domain.exchange.order.model;

import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    private static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    private Order limitBuyOrder(long quantity) {
        return new Order(
                "ORD-001",
                "ACC-001",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(quantity),
                new Price(60_000_000)
        );
    }

    @Test
    void cancel_shouldThrowWhenOrderAlreadyFilled() {
        Order order = limitBuyOrder(100);
        order.match(new Quantity(100));

        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void match_shouldSetStatusFilledWhenFullyMatched() {
        Order order = limitBuyOrder(100);

        order.match(new Quantity(100));

        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(order.getFilledQuantity().getValue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getRemainingQuantity().getValue()));
    }

    @Test
    void match_shouldSetStatusPartiallyFilledWhenPartialMatch() {
        Order order = limitBuyOrder(100);

        order.match(new Quantity(30));

        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(order.getFilledQuantity().getValue()));
        assertEquals(0, BigDecimal.valueOf(70).compareTo(order.getRemainingQuantity().getValue()));
    }

    @Test
    void placeOrder_shouldThrowWhenLimitOrderHasNoPrice() {
        assertThrows(IllegalArgumentException.class, () -> new Order(
                "ORD-001",
                "ACC-001",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(100),
                null
        ));
    }
}
