package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetOrderApplicationService {

    private final OrderRepository orderRepository;
    private final OwnershipChecker ownershipGuard;

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
