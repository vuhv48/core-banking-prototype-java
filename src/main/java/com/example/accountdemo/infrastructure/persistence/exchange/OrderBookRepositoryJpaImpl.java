package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.TradingPair;
import org.springframework.stereotype.Repository;

/**
 * Adapter triển khai OrderBookRepository bằng Spring Data JPA.
 */
@Repository
public class OrderBookRepositoryJpaImpl implements OrderBookRepository {

    private final OrderBookJpaRepository orderBookJpaRepository;
    private final OrderJpaRepository orderJpaRepository;
    private final OrderBookMapper orderBookMapper;
    private final OrderMapper orderMapper;

    public OrderBookRepositoryJpaImpl(
            OrderBookJpaRepository orderBookJpaRepository,
            OrderJpaRepository orderJpaRepository,
            OrderBookMapper orderBookMapper,
            OrderMapper orderMapper
    ) {
        this.orderBookJpaRepository = orderBookJpaRepository;
        this.orderJpaRepository = orderJpaRepository;
        this.orderBookMapper = orderBookMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * Tìm sổ lệnh theo cặp giao dịch.
     * - Tìm OrderBookJpaEntity theo baseCurrency + quoteCurrency
     * - Load tất cả OrderJpaEntity cùng cặp
     * - Map sang domain OrderBook (addOrder từng lệnh)
     */
    @Override
    public OrderBook findByTradingPair(TradingPair pair) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }

    /**
     * Lưu sổ lệnh.
     * - Lưu metadata OrderBookJpaEntity
     * - Lưu từng Order trong buyOrders + sellOrders qua OrderMapper
     */
    @Override
    public void save(OrderBook orderBook) {
        throw new UnsupportedOperationException("TODO: tự viết");
    }
}
