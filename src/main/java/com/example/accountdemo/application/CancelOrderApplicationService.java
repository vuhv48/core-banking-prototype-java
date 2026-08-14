package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.Money;
import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.OrderRepository;
import com.example.accountdemo.domain.exchange.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancelOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderBookRepository orderBookRepository;
    private final AccountRepository accountRepository;
    private final OwnershipChecker ownershipGuard;

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

        long remainingLock = order.getLockedAmountRemaining();
        if (remainingLock > 0 && order.getLockedCurrency() != null) {
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
