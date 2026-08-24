package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Money;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.orderbook.OrderBookRepository;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import com.example.accountdemo.domain.exchange.order.model.OrderStatus;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Application Service — use case hủy lệnh chưa khớp xong.
 * <p><b>Vì sao cần class này:</b> gom ownership, gỡ khỏi order book, và hoàn locked
 * trong một giao dịch — không để API tự xử lý từng bước.
 */
@Service
@RequiredArgsConstructor
public class CancelOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderBookRepository orderBookRepository;
    private final AccountRepository accountRepository;
    private final OwnershipChecker ownershipGuard;

    /**
     * Hủy lệnh: kiểm tra quyền → cancel domain → bỏ khỏi book → release số dư treo còn lại.
     */
    @Transactional
    public Order cancel(String username, String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new DomainException(ErrorStatus.ORDER_NOT_FOUND);
        }
        ownershipGuard.requireOrderAccess(username, order);

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new DomainException(ErrorStatus.ORDER_NOT_CANCELLABLE);
        }

        try {
            order.cancel();
        } catch (IllegalStateException e) {
            throw new DomainException(ErrorStatus.ORDER_NOT_CANCELLABLE, e.getMessage());
        }

        OrderBook orderBook = orderBookRepository.findByTradingPair(order.getTradingPair());
        if (orderBook != null && isOnBook(orderBook, orderId)) {
            orderBook.removeOrder(orderId);
            orderBookRepository.save(orderBook);
        }

        BigDecimal remainingLock = order.getLockedAmountRemaining();
        if (remainingLock.compareTo(BigDecimal.ZERO) > 0 && order.getLockedCurrency() != null) {
            Account account = accountRepository.findById(order.getAccountId());
            if (account == null) {
                throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND);
            }
            account.release(new Money(remainingLock, order.getLockedCurrency()));
            order.reduceLock(remainingLock);
            accountRepository.save(account);
        }

        orderRepository.save(order);
        return order;
    }

    private boolean isOnBook(OrderBook orderBook, String orderId) {
        return orderBook.getBuyOrders().stream().anyMatch(o -> orderId.equals(o.getOrderId()))
                || orderBook.getSellOrders().stream().anyMatch(o -> orderId.equals(o.getOrderId()));
    }
}
