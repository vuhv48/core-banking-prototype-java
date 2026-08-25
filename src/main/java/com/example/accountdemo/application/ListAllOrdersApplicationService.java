package com.example.accountdemo.application;

import com.example.accountdemo.domain.exchange.order.OrderPage;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin: list / tìm lệnh toàn hệ thống, có phân trang.
 */
@Service
@RequiredArgsConstructor
public class ListAllOrdersApplicationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OwnershipChecker ownershipGuard;

    @Transactional(readOnly = true)
    public OrderPage list(
            String username,
            Integer page,
            Integer size,
            String accountId,
            String orderId
    ) {
        ownershipGuard.requireAdmin(username);
        int p = page == null || page < 0 ? DEFAULT_PAGE : page;
        int s = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return orderRepository.findPage(p, s, blankToNull(accountId), blankToNull(orderId));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
