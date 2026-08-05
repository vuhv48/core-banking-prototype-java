package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi giữa domain OrderBook và OrderBookJpaEntity.
 */
@Component
public class OrderBookMapper {

    /**
     * Domain → JPA Entity (metadata sổ lệnh).
     * - entity.id = tradingPair.toString() hoặc base + "/" + quote
     * - entity.baseCurrency, quoteCurrency từ orderBook.getTradingPair()
     * Lưu ý: danh sách Order trong OrderBook lưu riêng qua OrderJpaEntity / OrderRepository.
     */
    public OrderBookJpaEntity toEntity(OrderBook orderBook) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }

    /**
     * JPA Entity + danh sách Order → Domain OrderBook.
     * - Tạo TradingPair từ entity
     * - Tạo OrderBook mới, addOrder từng order trong list
     */
    public OrderBook toDomain(OrderBookJpaEntity entity, List<Order> orders) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }
}
