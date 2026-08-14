package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Application Service — use case xem chi tiết một lệnh.
 * <p><b>Vì sao cần class này:</b> đọc Order kèm kiểm tra ownership,
 * tránh lộ lệnh của tài khoản khác qua API.
 */
@Service
@RequiredArgsConstructor
public class GetOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OwnershipChecker ownershipGuard;

    /**
     * Trả về Order nếu tồn tại và user được quyền xem.
     */
    @Transactional(readOnly = true)
    public Order get(String username, String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new DomainException(ErrorStatus.ORDER_NOT_FOUND);
        }
        ownershipGuard.requireOrderAccess(username, order);
        return order;
    }
}
