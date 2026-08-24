package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderStatus;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    void toEntity_shouldMapDomainFields() {
        Order order = ExchangeTestData.limitBuyOrder();

        OrderJpaEntity entity = orderMapper.toEntity(order);

        assertEquals("ORD-BUY-001", entity.getId());
        assertEquals("ACC-001", entity.getAccountId());
        assertEquals("BUY", entity.getSide());
        assertEquals("LIMIT", entity.getOrderType());
        assertEquals("BTC", entity.getBaseCurrency());
        assertEquals("VND", entity.getQuoteCurrency());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(entity.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(60_000_000L).compareTo(entity.getPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(entity.getFilledQuantity()));
        assertEquals("PENDING", entity.getStatus());
    }

    @Test
    void toDomain_shouldMapEntityFields() {
        OrderJpaEntity entity = ExchangeTestData.orderJpaEntity("ORD-SELL-001", "SELL", 61_000_000, 50);
        entity.setFilledQuantity(BigDecimal.valueOf(20));
        entity.setStatus(OrderStatus.PARTIALLY_FILLED.name());

        Order order = orderMapper.toDomain(entity);

        assertEquals("ORD-SELL-001", order.getOrderId());
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(OrderType.LIMIT, order.getOrderType());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(order.getQuantity().getValue()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(order.getFilledQuantity().getValue()));
        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        assertEquals(0, BigDecimal.valueOf(61_000_000L).compareTo(order.getPrice().getValue()));
    }

    @Test
    void roundTrip_shouldPreserveOrderState() {
        Order original = ExchangeTestData.limitBuyOrder();
        original.match(new com.example.accountdemo.domain.exchange.shared.Quantity(30));

        Order restored = orderMapper.toDomain(orderMapper.toEntity(original));

        assertEquals(original.getOrderId(), restored.getOrderId());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(restored.getFilledQuantity().getValue()));
        assertEquals(OrderStatus.PARTIALLY_FILLED, restored.getStatus());
    }
}
