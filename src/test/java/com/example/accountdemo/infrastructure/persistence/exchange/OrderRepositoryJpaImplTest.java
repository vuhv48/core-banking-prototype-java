package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryJpaImplTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    private OrderMapper orderMapper;
    private OrderRepositoryJpaImpl orderRepositoryJpaImpl;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();
        orderRepositoryJpaImpl = new OrderRepositoryJpaImpl(orderJpaRepository, orderMapper);
    }

    @Test
    void findById_shouldReturnDomainOrder() {
        OrderJpaEntity entity = ExchangeTestData.orderJpaEntity("ORD-BUY-001", "BUY", 60_000_000, 100);
        when(orderJpaRepository.findById("ORD-BUY-001")).thenReturn(Optional.of(entity));

        Order result = orderRepositoryJpaImpl.findById("ORD-BUY-001");

        assertEquals("ORD-BUY-001", result.getOrderId());
        assertEquals(OrderSide.BUY, result.getSide());
    }

    @Test
    void findById_shouldReturnNullWhenNotFound() {
        when(orderJpaRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertNull(orderRepositoryJpaImpl.findById("UNKNOWN"));
    }

    @Test
    void findById_shouldIgnoreDeletedEntity() {
        OrderJpaEntity entity = ExchangeTestData.orderJpaEntity("ORD-BUY-001", "BUY", 60_000_000, 100);
        entity.setDeleted(true);
        when(orderJpaRepository.findById("ORD-BUY-001")).thenReturn(Optional.of(entity));

        assertNull(orderRepositoryJpaImpl.findById("ORD-BUY-001"));
    }

    @Test
    void save_shouldPersistMappedEntity() {
        Order order = ExchangeTestData.limitBuyOrder();
        when(orderJpaRepository.findById("ORD-BUY-001")).thenReturn(Optional.empty());

        orderRepositoryJpaImpl.save(order);

        ArgumentCaptor<OrderJpaEntity> captor = ArgumentCaptor.forClass(OrderJpaEntity.class);
        verify(orderJpaRepository).save(captor.capture());
        OrderJpaEntity saved = captor.getValue();
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(false, saved.isDeleted());
        assertEquals(OrderStatus.PENDING.name(), saved.getStatus());
        assertEquals("ORD-BUY-001", saved.getId());
    }
}
