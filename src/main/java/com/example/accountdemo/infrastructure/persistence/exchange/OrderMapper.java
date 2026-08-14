package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderStatus;
import com.example.accountdemo.domain.exchange.OrderType;
import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.TradingPair;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi giữa domain Order và OrderJpaEntity.
 */
@Component
public class OrderMapper {

    /**
     * Domain → JPA Entity (khi save).
     */
    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getOrderId());
        entity.setAccountId(order.getAccountId());
        entity.setSide(order.getSide().name());
        entity.setOrderType(order.getOrderType().name());
        entity.setBaseCurrency(order.getTradingPair().getBaseCurrency());
        entity.setQuoteCurrency(order.getTradingPair().getQuoteCurrency());
        entity.setQuantity(order.getQuantity().getValue());
        entity.setPrice(order.getPrice() != null ? order.getPrice().getValue() : null);
        entity.setFilledQuantity(order.getFilledQuantity().getValue());
        entity.setStatus(order.getStatus().name());
        entity.setLockedCurrency(order.getLockedCurrency());
        entity.setLockedAmountRemaining(order.getLockedAmountRemaining());
        return entity;
    }

    /**
     * JPA Entity → Domain (khi load).
     */
    public Order toDomain(OrderJpaEntity entity) {
        TradingPair tradingPair = new TradingPair(entity.getBaseCurrency(), entity.getQuoteCurrency());
        Quantity quantity = new Quantity(entity.getQuantity());
        Price price = entity.getPrice() != null ? new Price(entity.getPrice()) : null;
        Quantity filledQuantity = new Quantity(entity.getFilledQuantity());

        return Order.reconstitute(
                entity.getId(),
                entity.getAccountId(),
                OrderSide.valueOf(entity.getSide()),
                OrderType.valueOf(entity.getOrderType()),
                tradingPair,
                quantity,
                price,
                filledQuantity,
                OrderStatus.valueOf(entity.getStatus()),
                entity.getLockedCurrency(),
                entity.getLockedAmountRemaining()
        );
    }
}
