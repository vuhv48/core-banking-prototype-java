package com.example.accountdemo.domain.exchange;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMatchingServiceTest {

    private static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    private OrderMatchingService matchingService;
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        matchingService = new OrderMatchingService();
        orderBook = new OrderBook(BTC_VND);
    }

    private Order buy(String id, long qty, long price) {
        return new Order(id, "ACC-1", OrderSide.BUY, OrderType.LIMIT, BTC_VND, new Quantity(qty), new Price(price));
    }

    private Order sell(String id, long qty, long price) {
        return new Order(id, "ACC-2", OrderSide.SELL, OrderType.LIMIT, BTC_VND, new Quantity(qty), new Price(price));
    }

    @Test
    void match_shouldFullyMatchWhenPriceCrosses() {
        orderBook.addOrder(sell("SELL-1", 10, 60_000_000));
        Order buy = buy("BUY-1", 10, 60_000_000);

        MatchResult result = matchingService.match(buy, orderBook);

        assertEquals(1, result.getTrades().size());
        assertEquals(OrderStatus.FILLED, buy.getStatus());
        assertEquals(OrderStatus.FILLED, result.getAffectedOrders().stream()
                .filter(o -> o.getOrderId().equals("SELL-1")).findFirst().orElseThrow().getStatus());
        assertTrue(orderBook.getBuyOrders().isEmpty());
        assertTrue(orderBook.getSellOrders().isEmpty());
    }

    @Test
    void match_shouldPartiallyMatchWhenQuantityDiffers() {
        orderBook.addOrder(sell("SELL-1", 30, 60_000_000));
        Order buy = buy("BUY-1", 50, 60_000_000);

        MatchResult result = matchingService.match(buy, orderBook);

        assertEquals(1, result.getTrades().size());
        assertEquals(30, result.getTrades().get(0).getMatchedQuantity().getValue());
        assertEquals(OrderStatus.PARTIALLY_FILLED, buy.getStatus());
        assertEquals(20, buy.getRemainingQuantity().getValue());
        assertEquals(OrderStatus.FILLED, result.getAffectedOrders().stream()
                .filter(o -> o.getOrderId().equals("SELL-1")).findFirst().orElseThrow().getStatus());
        assertEquals(1, orderBook.getBuyOrders().size());
        assertTrue(orderBook.getSellOrders().isEmpty());
    }

    @Test
    void match_shouldNotMatchWhenPriceDoesNotCross() {
        orderBook.addOrder(sell("SELL-1", 10, 61_000_000));
        Order buy = buy("BUY-1", 10, 60_000_000);

        MatchResult result = matchingService.match(buy, orderBook);

        assertTrue(result.getTrades().isEmpty());
        assertEquals(OrderStatus.PENDING, buy.getStatus());
        assertEquals(1, orderBook.getBuyOrders().size());
        assertEquals(1, orderBook.getSellOrders().size());
    }

    @Test
    void match_shouldMatchAgainstBestPriceFirst() {
        orderBook.addOrder(sell("SELL-HIGH", 10, 62_000_000));
        orderBook.addOrder(sell("SELL-LOW", 10, 60_000_000));
        Order buy = buy("BUY-1", 10, 62_000_000);

        MatchResult result = matchingService.match(buy, orderBook);

        assertEquals(1, result.getTrades().size());
        assertEquals("SELL-LOW", result.getTrades().get(0).getSellOrderId());
        assertEquals(60_000_000, result.getTrades().get(0).getMatchedPrice().getValue());
    }

    @Test
    void match_shouldMatchMultipleOrdersWhenOneLargeOrderComesIn() {
        orderBook.addOrder(sell("SELL-1", 10, 60_000_000));
        orderBook.addOrder(sell("SELL-2", 15, 60_500_000));
        orderBook.addOrder(sell("SELL-3", 20, 61_000_000));
        Order buy = buy("BUY-1", 25, 61_000_000);

        MatchResult result = matchingService.match(buy, orderBook);

        assertEquals(2, result.getTrades().size());
        assertEquals(OrderStatus.FILLED, buy.getStatus());
        assertEquals(0, buy.getRemainingQuantity().getValue());
        assertEquals(1, orderBook.getSellOrders().size());
        assertEquals("SELL-3", orderBook.getSellOrders().get(0).getOrderId());
    }

    @Test
    void match_shouldLeaveUnmatchedWhenOrderBookHasNoOpposite() {
        Order buy = buy("BUY-1", 10, 60_000_000);

        MatchResult result = matchingService.match(buy, orderBook);

        assertTrue(result.getTrades().isEmpty());
        assertEquals(OrderStatus.PENDING, buy.getStatus());
        assertEquals(List.of("BUY-1"), orderBook.getBuyOrders().stream().map(Order::getOrderId).toList());
    }
}
