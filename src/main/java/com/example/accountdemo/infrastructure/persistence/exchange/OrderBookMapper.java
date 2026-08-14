package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Map domain {@code OrderBook} ↔ {@link OrderBookJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> tách metadata sổ lệnh khỏi danh sách Order (load riêng rồi ghép).
 */
@Component
public class OrderBookMapper {

    /**
     * Domain → JPA Entity (metadata sổ lệnh).
     * Danh sách Order lưu riêng qua OrderJpaEntity / OrderRepository.
     */
    public OrderBookJpaEntity toEntity(OrderBook orderBook) {
        TradingPair tradingPair = orderBook.getTradingPair();
        OrderBookJpaEntity entity = new OrderBookJpaEntity();
        entity.setId(tradingPair.toString());
        entity.setBaseCurrency(tradingPair.getBaseCurrency());
        entity.setQuoteCurrency(tradingPair.getQuoteCurrency());
        return entity;
    }

    /**
     * JPA Entity + danh sách Order → Domain OrderBook.
     */
    public OrderBook toDomain(OrderBookJpaEntity entity, List<Order> orders) {
        TradingPair tradingPair = new TradingPair(entity.getBaseCurrency(), entity.getQuoteCurrency());
        OrderBook orderBook = new OrderBook(tradingPair);
        if (orders != null) {
            for (Order order : orders) {
                orderBook.addOrder(order);
            }
        }
        return orderBook;
    }
}
