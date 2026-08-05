package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderRepository;
import org.springframework.stereotype.Repository;

/**
 * Adapter triển khai OrderRepository bằng Spring Data JPA.
 */
@Repository
public class OrderRepositoryJpaImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    public OrderRepositoryJpaImpl(OrderJpaRepository orderJpaRepository, OrderMapper orderMapper) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * Tìm lệnh theo ID.
     * - orderJpaRepository.findById(orderId)
     * - map sang domain, bỏ qua bản ghi deleted
     * - không có → null
     */
    @Override
    public Order findById(String orderId) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }

    /**
     * Lưu lệnh.
     * - orderMapper.toEntity(order) → save
     * - xử lý audit fields (createdAt, updatedAt...) giống AccountRepositoryJpaImpl
     */
    @Override
    public void save(Order order) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }
}
