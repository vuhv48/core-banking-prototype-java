package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

/**
 * Adapter triển khai {@code OrderRepository} bằng Spring Data JPA.
 *
 * <p><b>Vì sao cần class này:</b> đóng port domain Order; application không phụ thuộc Spring Data.
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryJpaImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderMapper orderMapper;

    /** Tìm lệnh theo id (bỏ soft-deleted). */
    @Override
    public Order findById(String orderId) {
        return orderJpaRepository.findById(orderId)
                .filter(entity -> !entity.isDeleted())
                .map(orderMapper::toDomain)
                .orElse(null);
    }

    /** Insert hoặc cập nhật lệnh (giữ audit created*). */
    @Override
    public void save(Order order) {
        OrderJpaEntity entity = orderMapper.toEntity(order);
        LocalDateTime now = LocalDateTime.now();

        Optional<OrderJpaEntity> existing = orderJpaRepository.findById(order.getOrderId());
        if (existing.isPresent()) {
            OrderJpaEntity current = existing.get();
            entity.setDeleted(current.isDeleted());
            entity.setCreatedAt(current.getCreatedAt());
            entity.setCreatedBy(current.getCreatedBy());
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(current.getUpdatedBy());
        } else {
            entity.setDeleted(false);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
        }

        orderJpaRepository.save(entity);
    }
}
