package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.OrderPage;
import com.example.accountdemo.domain.exchange.order.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Override
    public List<Order> findByAccountId(String accountId) {
        return orderJpaRepository.findByAccountIdAndDeletedFalse(accountId).stream()
                .map(orderMapper::toDomain)
                .toList();
    }

    @Override
    public OrderPage findPage(int page, int size, String accountId, String orderId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderJpaEntity> result = orderJpaRepository.search(
                toContainsPattern(accountId),
                toContainsPattern(orderId),
                pageable
        );
        List<Order> content = result.getContent().stream()
                .map(orderMapper::toDomain)
                .toList();
        return new OrderPage(content, page, size, result.getTotalElements());
    }

    /** "" = không lọc; ngược lại pattern LIKE %value% (chữ thường). */
    private static String toContainsPattern(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "%" + value.trim().toLowerCase() + "%";
    }
}
