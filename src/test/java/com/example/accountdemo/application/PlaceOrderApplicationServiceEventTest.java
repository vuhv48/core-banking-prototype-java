package com.example.accountdemo.application;

import com.example.accountdemo.domain.exchange.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.OrderMatchingService;
import com.example.accountdemo.domain.exchange.OrderRepository;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderStatus;
import com.example.accountdemo.domain.exchange.OrderType;
import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.TradingPair;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5 — fake publisher + fake repo (HashMap), không cần Spring/DB.
 */
class PlaceOrderApplicationServiceEventTest {

    private static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    private FakeOrderRepository orderRepository;
    private FakeOrderBookRepository orderBookRepository;
    private FakeDomainEventPublisher eventPublisher;
    private PlaceOrderApplicationService placeOrderApplicationService;

    @BeforeEach
    void setUp() {
        orderRepository = new FakeOrderRepository();
        orderBookRepository = new FakeOrderBookRepository();
        eventPublisher = new FakeDomainEventPublisher();
        placeOrderApplicationService = new PlaceOrderApplicationService(
                orderRepository,
                orderBookRepository,
                new OrderMatchingService(),
                eventPublisher
        );

        // Admin đã mở cặp + sẵn 1 lệnh bán trên sổ
        OrderBook book = new OrderBook(BTC_VND);
        book.addOrder(new Order(
                "SELL-1", "ACC-2", OrderSide.SELL, OrderType.LIMIT,
                BTC_VND, new Quantity(10), new Price(60_000_000)
        ));
        orderBookRepository.save(book);
        orderRepository.save(book.getSellOrders().get(0));
    }

    @Test
    void placeOrder_shouldPublishEventWhenTradeExecuted() {
        Order buy = placeOrderApplicationService.placeOrder(
                "ACC-1",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(10),
                new Price(60_000_000)
        );

        assertEquals(OrderStatus.FILLED, buy.getStatus());
        assertEquals(1, eventPublisher.events.size());
        TradeExecutedEvent event = eventPublisher.events.get(0);
        assertEquals("SELL-1", event.getSellOrderId());
        assertEquals(buy.getOrderId(), event.getBuyOrderId());
        assertEquals(10, event.getQuantity().getValue());
        assertEquals(60_000_000, event.getPrice().getValue());
    }

    @Test
    void placeOrder_shouldNotPublishEventWhenNoMatch() {
        placeOrderApplicationService.placeOrder(
                "ACC-1",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(10),
                new Price(50_000_000)
        );

        assertTrue(eventPublisher.events.isEmpty());
    }

    private static final class FakeDomainEventPublisher implements DomainEventPublisher {
        private final List<TradeExecutedEvent> events = new ArrayList<>();

        @Override
        public void publish(TradeExecutedEvent event) {
            events.add(event);
        }
    }

    private static final class FakeOrderRepository implements OrderRepository {
        private final Map<String, Order> store = new HashMap<>();

        @Override
        public Order findById(String orderId) {
            return store.get(orderId);
        }

        @Override
        public void save(Order order) {
            store.put(order.getOrderId(), order);
        }
    }

    private static final class FakeOrderBookRepository implements OrderBookRepository {
        private final Map<String, OrderBook> store = new HashMap<>();

        @Override
        public OrderBook findByTradingPair(TradingPair pair) {
            return store.get(pair.toString());
        }

        @Override
        public void save(OrderBook orderBook) {
            store.put(orderBook.getTradingPair().toString(), orderBook);
        }
    }
}
