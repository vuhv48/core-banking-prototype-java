package com.example.accountdemo.domain.exchange.orderbook;

import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.shared.TradingPair;

/**
 * Port (Repository) — persistence sổ {@link OrderBook} theo {@link TradingPair}.
 *
 * <p>Load sổ (kèm lệnh đang chờ) / save sau match. Implement JPA ở infrastructure.
 */
public interface OrderBookRepository {

    OrderBook findByTradingPair(TradingPair pair);

    void save(OrderBook orderBook);
}
