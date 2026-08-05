package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi giữa domain Order và OrderJpaEntity.
 */
@Component
public class OrderMapper {

    /**
     * Domain → JPA Entity (khi save).
     * - entity.id = order.getOrderId()
     * - entity.accountId, side, orderType, status = .name()
     * - entity.baseCurrency / quoteCurrency từ order.getTradingPair()
     * - entity.quantity = order.getQuantity().getValue()
     * - entity.price = order.getPrice() != null ? order.getPrice().getValue() : null
     * - entity.filledQuantity = order.getFilledQuantity().getValue() (cần thêm getter trên Order nếu chưa có)
     */
    public OrderJpaEntity toEntity(Order order) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }

    /**
     * JPA Entity → Domain (khi load).
     * - Tạo TradingPair từ baseCurrency + quoteCurrency
     * - Tạo Quantity, Price (nullable nếu MARKET)
     * - Khôi phục filledQuantity và status từ entity
     * Gợi ý: có thể cần factory method Order.reconstitute(...) trên domain để load từ DB.
     */
    public Order toDomain(OrderJpaEntity entity) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }
}
