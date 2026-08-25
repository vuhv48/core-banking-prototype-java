package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountPage;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import com.example.accountdemo.domain.exchange.event.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade;
import com.example.accountdemo.domain.exchange.order.OrderPage;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.orderbook.OrderBookRepository;
import com.example.accountdemo.domain.exchange.matching.OrderMatchingService;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderStatus;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.trade.TradeRepository;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceOrderApplicationServiceEventTest {

    private static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    private FakeOrderRepository orderRepository;
    private FakeOrderBookRepository orderBookRepository;
    private FakeAccountRepository accountRepository;
    private FakeTradeRepository tradeRepository;
    private FakeDomainEventPublisher eventPublisher;
    private PlaceOrderApplicationService placeOrderApplicationService;

    @BeforeEach
    void setUp() {
        orderRepository = new FakeOrderRepository();
        orderBookRepository = new FakeOrderBookRepository();
        accountRepository = new FakeAccountRepository();
        tradeRepository = new FakeTradeRepository();
        eventPublisher = new FakeDomainEventPublisher();

        TradeSettlementService settlementService =
                new TradeSettlementService(accountRepository, tradeRepository);
        OwnershipChecker allowAll = new OwnershipChecker() {
            @Override
            public void requireAccountAccess(String username, String accountId) {
            }

            @Override
            public void requireAdmin(String username) {
            }

            @Override
            public void requireOrderAccess(String username, Order order) {
            }
        };

        placeOrderApplicationService = new PlaceOrderApplicationService(
                orderRepository,
                orderBookRepository,
                new OrderMatchingService(),
                eventPublisher,
                accountRepository,
                settlementService,
                allowAll
        );

        Map<String, Balance> acc1 = new LinkedHashMap<>();
        acc1.put("VND", new Balance("VND", 1_000_000_000L, 0));
        acc1.put("BTC", new Balance("BTC", 0, 0));
        accountRepository.save(new Account("ACC-1", AccountStatus.ACTIVE, acc1));

        Map<String, Balance> acc2 = new LinkedHashMap<>();
        acc2.put("VND", new Balance("VND", 0, 0));
        acc2.put("BTC", new Balance("BTC", 100, 0));
        accountRepository.save(new Account("ACC-2", AccountStatus.ACTIVE, acc2));

        orderBookRepository.save(new OrderBook(BTC_VND));

        // Đặt SELL qua service để có lock đúng
        placeOrderApplicationService.placeOrder(
                "seller",
                "ACC-2",
                OrderSide.SELL,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(10),
                new Price(60_000_000)
        );
    }

    @Test
    void placeOrder_shouldPublishEventWhenTradeExecuted() {
        Order buy = placeOrderApplicationService.placeOrder(
                "buyer",
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
        assertEquals(buy.getOrderId(), event.getBuyOrderId());
        assertEquals(0, BigDecimal.valueOf(10).compareTo(event.getQuantity().getValue()));
        assertEquals(0, BigDecimal.valueOf(60_000_000).compareTo(event.getPrice().getValue()));
        assertEquals(1, tradeRepository.trades.size());

        Account buyer = accountRepository.findById("ACC-1");
        Account seller = accountRepository.findById("ACC-2");
        assertEquals(0, BigDecimal.valueOf(10).compareTo(buyer.getAvailable("BTC").getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyer.getLocked("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(600_000_000L).compareTo(seller.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(90).compareTo(seller.getAvailable("BTC").getAmount()));
    }

    @Test
    void placeOrder_shouldNotPublishEventWhenNoMatch() {
        placeOrderApplicationService.placeOrder(
                "buyer",
                "ACC-1",
                OrderSide.BUY,
                OrderType.LIMIT,
                BTC_VND,
                new Quantity(10),
                new Price(50_000_000)
        );

        assertTrue(eventPublisher.events.isEmpty());
        assertTrue(tradeRepository.trades.isEmpty());
        assertEquals(0, BigDecimal.valueOf(500_000_000L).compareTo(accountRepository.findById("ACC-1").getLocked("VND").getAmount()));
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

        @Override
        public List<Order> findByAccountId(String accountId) {
            return List.of();
        }

        @Override
        public OrderPage findPage(int page, int size, String accountId, String orderId) {
            return new OrderPage(List.of(), page, size, 0);
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

    private static final class FakeAccountRepository implements AccountRepository {
        private final Map<String, Account> store = new HashMap<>();

        @Override
        public Account findById(String accountId) {
            return store.get(accountId);
        }

        @Override
        public void save(Account account) {
            store.put(account.getAccountId(), account);
        }

        @Override
        public List<Account> findAll() {
            return List.of();
        }

        @Override
        public AccountPage findPage(int page, int size) {
            return new AccountPage(List.of(), page, size, 0);
        }
    }

    private static final class FakeTradeRepository implements TradeRepository {
        private final List<ExecutedTrade> trades = new ArrayList<>();

        @Override
        public void save(ExecutedTrade trade) {
            trades.add(trade);
        }
    }
}
