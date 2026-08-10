package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.TradingPair;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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

    @Override
    public OrderBook findByTradingPair(TradingPair pair) {
        return orderBookJpaRepository
                .findByBaseCurrencyAndQuoteCurrency(pair.getBaseCurrency(), pair.getQuoteCurrency())
                .filter(entity -> !entity.isDeleted())
                .map(entity -> {
                    List<Order> orders = orderJpaRepository
                            .findByBaseCurrencyAndQuoteCurrency(pair.getBaseCurrency(), pair.getQuoteCurrency())
                            .stream()
                            .filter(orderEntity -> !orderEntity.isDeleted())
                            .map(orderMapper::toDomain)
                            .toList();
                    return orderBookMapper.toDomain(entity, orders);
                })
                .orElse(null);
    }

    @Override
    public void save(OrderBook orderBook) {
        OrderBookJpaEntity entity = orderBookMapper.toEntity(orderBook);
        LocalDateTime now = LocalDateTime.now();

        Optional<OrderBookJpaEntity> existing = orderBookJpaRepository.findById(entity.getId());
        if (existing.isPresent()) {
            OrderBookJpaEntity current = existing.get();
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

        orderBookJpaRepository.save(entity);

        Stream.concat(orderBook.getBuyOrders().stream(), orderBook.getSellOrders().stream())
                .forEach(this::saveOrder);
    }

    private void saveOrder(Order order) {
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
