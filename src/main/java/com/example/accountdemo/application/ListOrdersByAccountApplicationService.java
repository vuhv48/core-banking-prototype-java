package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import com.example.accountdemo.domain.exchange.order.model.Order;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service — use case xem danh sách lệnh của một tài khoản.
 */
@Service
@RequiredArgsConstructor
public class ListOrdersByAccountApplicationService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final OwnershipChecker ownershipGuard;

    @Transactional(readOnly = true)
    public List<Order> getOrdersByAccountId(String username, String accountId) {
        ownershipGuard.requireAccountAccess(username, accountId);
        if (accountRepository.findById(accountId) == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND);
        }
        return orderRepository.findByAccountId(accountId);
    }
}
