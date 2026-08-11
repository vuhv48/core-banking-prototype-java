package com.example.accountdemo.application;

import com.example.accountdemo.domain.exchange.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.MatchResult;
import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.OrderMatchingService;
import com.example.accountdemo.domain.exchange.OrderRepository;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderType;
import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.Trade;
import com.example.accountdemo.domain.exchange.TradingPair;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application Service — điều phối use case "đặt lệnh", không chứa rule khớp lệnh.
 *
 * <p>Phân loại DDD:
 * <ul>
 *   <li>Không phải Aggregate / Domain Service — chỉ orchestrate: load → gọi domain → save → publish</li>
 *   <li>Rule nghiệp vụ nằm ở {@link com.example.accountdemo.domain.exchange.Order},
 *       {@link com.example.accountdemo.domain.exchange.OrderBook},
 *       {@link com.example.accountdemo.domain.exchange.OrderMatchingService}</li>
 * </ul>
 *
 * <p>Luồng:
 * 1. Tạo lệnh mới (Order tự validate LIMIT phải có giá)
 * 2. Sổ cặp phải đã được admin/seed mở
 * 3. Thử khớp với lệnh đang chờ trên sổ
 * 4. Lưu DB các lệnh đã đổi + sổ
 * 5. Mỗi lần khớp thành công → publish TradeExecutedEvent (log)
 */
@Service
public class PlaceOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderBookRepository orderBookRepository;
    private final OrderMatchingService orderMatchingService;
    private final DomainEventPublisher domainEventPublisher;

    public PlaceOrderApplicationService(
            OrderRepository orderRepository,
            OrderBookRepository orderBookRepository,
            OrderMatchingService orderMatchingService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.orderBookRepository = orderBookRepository;
        this.orderMatchingService = orderMatchingService;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * Đặt lệnh mua/bán trên một cặp (vd BTC/VND).
     *
     * @return lệnh sau khi xử lý — status có thể:
     *         PENDING (chưa khớp, đang chờ trên sổ),
     *         PARTIALLY_FILLED (khớp một phần),
     *         FILLED (khớp hết),
     *         CANCELLED (MARKET còn dư bị hủy)
     */
    public Order placeOrder(
            String accountId,
            OrderSide side,
            OrderType orderType,
            TradingPair tradingPair,
            Quantity quantity,
            Price price
    ) {
        // Lệnh mới của user (vd Anh A mua 10 @ 60M)
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, accountId, side, orderType, tradingPair, quantity, price);

        // ── Bước 1: LOAD DB → object Java (list buyOrders/sellOrders trong OrderBook) ──
        OrderBook orderBook = orderBookRepository.findByTradingPair(tradingPair);
        if (orderBook == null) {
            throw new IllegalArgumentException("Cặp giao dịch chưa được mở: " + tradingPair);
        }

        // ── Bước 2: DOMAIN — khớp trên RAM (addOrder/removeOrder/match), chưa ghi DB ──
        MatchResult matchResult = orderMatchingService.match(order, orderBook);

        // ── Bước 3: LƯU DB (bước match KHÔNG làm việc này) ──
        // Mỗi Order trong affected = 1 dòng bảng orders (INSERT/UPDATE filled_quantity, status...)
        // Vd: mua 10 khớp 8 còn 2 → BUY-NEW: quantity=10, filled=8, PARTIALLY_FILLED
        for (Order affected : matchResult.getAffectedOrders()) {
            orderRepository.save(affected);
        }
        // Lưu các lệnh đang nằm TRONG list buyOrders + sellOrders của sổ (RAM → DB)
        // Lệnh vừa addOrder (còn 2 đồng treo sổ) nằm trong list này → được persist để lần sau load lại
        orderBookRepository.save(orderBook);

        // Thông báo mỗi lần khớp (Sprint 5) — save xong mới publish
        for (Trade trade : matchResult.getTrades()) {
            domainEventPublisher.publish(toEvent(trade, tradingPair));
        }

        return order;
    }

    private TradeExecutedEvent toEvent(Trade trade, TradingPair tradingPair) {
        return new TradeExecutedEvent(
                UUID.randomUUID().toString(),
                trade.getBuyOrderId(),
                trade.getSellOrderId(),
                tradingPair,
                trade.getMatchedQuantity(),
                trade.getMatchedPrice(),
                Instant.now()
        );
    }
}
