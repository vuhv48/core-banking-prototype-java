package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.TradingPair;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderBookRepositoryJpaImplTest {

    @Mock
    private OrderBookJpaRepository orderBookJpaRepository;

    @Mock
    private OrderJpaRepository orderJpaRepository;

    private OrderBookMapper orderBookMapper;
    private OrderMapper orderMapper;
    private OrderBookRepositoryJpaImpl orderBookRepositoryJpaImpl;

    @BeforeEach
    void setUp() {
        orderBookMapper = new OrderBookMapper();
        orderMapper = new OrderMapper();
        orderBookRepositoryJpaImpl = new OrderBookRepositoryJpaImpl(
                orderBookJpaRepository,
                orderJpaRepository,
                orderBookMapper,
                orderMapper
        );
    }

    @Test
    void findByTradingPair_shouldReturnOrderBookWithOrders() {
        TradingPair pair = ExchangeTestData.BTC_VND;
        OrderBookJpaEntity bookEntity = ExchangeTestData.orderBookJpaEntity();
        OrderJpaEntity buyEntity = ExchangeTestData.orderJpaEntity("ORD-BUY-001", "BUY", 60_000_000, 100);
        OrderJpaEntity sellEntity = ExchangeTestData.orderJpaEntity("ORD-SELL-001", "SELL", 61_000_000, 50);

        when(orderBookJpaRepository.findByBaseCurrencyAndQuoteCurrency("BTC", "VND"))
                .thenReturn(Optional.of(bookEntity));
        when(orderJpaRepository.findByBaseCurrencyAndQuoteCurrency("BTC", "VND"))
                .thenReturn(List.of(buyEntity, sellEntity));

        OrderBook result = orderBookRepositoryJpaImpl.findByTradingPair(pair);

        assertNotNull(result);
        assertEquals("BTC/VND", result.getTradingPair().toString());
        assertEquals(1, result.getBuyOrders().size());
        assertEquals(1, result.getSellOrders().size());
        assertEquals("ORD-BUY-001", result.getBuyOrders().get(0).getOrderId());
        assertEquals("ORD-SELL-001", result.getSellOrders().get(0).getOrderId());
    }

    @Test
    void findByTradingPair_shouldReturnNullWhenNotFound() {
        when(orderBookJpaRepository.findByBaseCurrencyAndQuoteCurrency("BTC", "VND"))
                .thenReturn(Optional.empty());

        assertNull(orderBookRepositoryJpaImpl.findByTradingPair(ExchangeTestData.BTC_VND));
    }

    @Test
    void save_shouldPersistOrderBookAndOrders() {
        OrderBook orderBook = ExchangeTestData.sampleOrderBook();
        when(orderBookJpaRepository.findById("BTC/VND")).thenReturn(Optional.empty());
        when(orderJpaRepository.findById(any())).thenReturn(Optional.empty());

        orderBookRepositoryJpaImpl.save(orderBook);

        verify(orderBookJpaRepository).save(any(OrderBookJpaEntity.class));
        verify(orderJpaRepository, atLeastOnce()).save(any(OrderJpaEntity.class));
    }
}
