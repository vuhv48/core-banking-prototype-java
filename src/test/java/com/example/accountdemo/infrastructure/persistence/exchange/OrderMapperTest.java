package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderStatus;
import com.example.accountdemo.domain.exchange.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals(100, entity.getQuantity());
        assertEquals(60_000_000L, entity.getPrice());
        assertEquals(0, entity.getFilledQuantity());
        assertEquals("PENDING", entity.getStatus());
    }

    @Test
    void toDomain_shouldMapEntityFields() {
        OrderJpaEntity entity = ExchangeTestData.orderJpaEntity("ORD-SELL-001", "SELL", 61_000_000, 50);
        entity.setFilledQuantity(20);
        entity.setStatus(OrderStatus.PARTIALLY_FILLED.name());

        Order order = orderMapper.toDomain(entity);

        assertEquals("ORD-SELL-001", order.getOrderId());
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(OrderType.LIMIT, order.getOrderType());
        assertEquals(50, order.getQuantity().getValue());
        assertEquals(20, order.getFilledQuantity().getValue());
        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        assertEquals(61_000_000L, order.getPrice().getValue());
    }

    @Test
    void roundTrip_shouldPreserveOrderState() {
        Order original = ExchangeTestData.limitBuyOrder();
        original.match(new com.example.accountdemo.domain.exchange.Quantity(30));

        Order restored = orderMapper.toDomain(orderMapper.toEntity(original));

        assertEquals(original.getOrderId(), restored.getOrderId());
        assertEquals(30, restored.getFilledQuantity().getValue());
        assertEquals(OrderStatus.PARTIALLY_FILLED, restored.getStatus());
    }
}
