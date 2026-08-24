package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Money;
import com.example.accountdemo.domain.exchange.event.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.matching.MatchResult;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.orderbook.OrderBookRepository;
import com.example.accountdemo.domain.exchange.matching.OrderMatchingService;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.matching.Trade;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Application Service — use case đặt lệnh (LIMIT/MARKET).
 * <p><b>Vì sao cần class này:</b> điều phối ownership → reserve ví → khớp sổ lệnh →
 * settle → publish event trong một transaction; API không được tự ghép các bước này.
 */
@Service
@RequiredArgsConstructor
public class PlaceOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderBookRepository orderBookRepository;
    private final OrderMatchingService orderMatchingService;
    private final DomainEventPublisher domainEventPublisher;
    private final AccountRepository accountRepository;
    private final TradeSettlementService tradeSettlementService;
    private final OwnershipChecker ownershipGuard;

    /**
     * Đặt lệnh: treo số dư, khớp ngay nếu được, tất toán trade, trả Order sau khi persist.
     */
    @Transactional
    public Order placeOrder(
            String username,
            String accountId,
            OrderSide side,
            OrderType orderType,
            TradingPair tradingPair,
            Quantity quantity,
            Price price
    ) {
        ownershipGuard.requireAccountAccess(username, accountId);

        if (side == OrderSide.BUY && orderType == OrderType.MARKET) {
            throw new DomainException(ErrorStatus.MARKET_BUY_NOT_SUPPORTED);
        }

        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản: " + accountId);
        }

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, accountId, side, orderType, tradingPair, quantity, price);

        Money lockMoney = calculateLock(order);
        try {
            account.reserve(lockMoney);
        } catch (IllegalStateException e) {
            throw new DomainException(ErrorStatus.ACCOUNT_FROZEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorStatus.INSUFFICIENT_BALANCE, e.getMessage());
        }
        order.initializeLock(lockMoney.getCurrency(), lockMoney.getAmount());
        accountRepository.save(account);

        OrderBook orderBook = orderBookRepository.findByTradingPair(tradingPair);
        if (orderBook == null) {
            throw new DomainException(ErrorStatus.ORDER_BOOK_NOT_OPEN, "Cặp giao dịch chưa được mở: " + tradingPair);
        }

        MatchResult matchResult = orderMatchingService.match(order, orderBook);

        Map<String, Order> ordersById = new HashMap<>();
        for (Order affected : matchResult.getAffectedOrders()) {
            ordersById.put(affected.getOrderId(), affected);
        }

        for (Trade trade : matchResult.getTrades()) {
            tradeSettlementService.settle(trade, tradingPair, ordersById);
        }

        // MARKET hủy phần dư → trả locked còn lại
        releaseCancelledRemainder(order);

        for (Order affected : matchResult.getAffectedOrders()) {
            orderRepository.save(affected);
        }
        orderBookRepository.save(orderBook);

        for (Trade trade : matchResult.getTrades()) {
            domainEventPublisher.publish(toEvent(trade, tradingPair));
        }

        return order;
    }

    private void releaseCancelledRemainder(Order order) {
        if (order.getStatus() != com.example.accountdemo.domain.exchange.order.model.OrderStatus.CANCELLED) {
            return;
        }
        BigDecimal remaining = order.getLockedAmountRemaining();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0 || order.getLockedCurrency() == null) {
            return;
        }
        Account account = accountRepository.findById(order.getAccountId());
        if (account == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND);
        }
        account.release(new Money(remaining, order.getLockedCurrency()));
        order.reduceLock(remaining);
        accountRepository.save(account);
    }

    private Money calculateLock(Order order) {
        TradingPair pair = order.getTradingPair();
        if (order.getSide() == OrderSide.BUY) {
            BigDecimal amount = order.getQuantity().getValue().multiply(order.getPrice().getValue());
            return new Money(amount, pair.getQuoteCurrency());
        }
        return new Money(order.getQuantity().getValue(), pair.getBaseCurrency());
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
