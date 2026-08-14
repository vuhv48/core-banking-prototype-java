package com.example.accountdemo.domain.exchange.orderbook;

import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.shared.TradingPair;

/**
 * Port (Repository) — persistence sổ {@link OrderBook} theo {@link TradingPair}.
 *
 * <p><b>Vì sao cần:</b> load sổ (kèm lệnh đang chờ) / save sau match mà domain
 * không phụ thuộc JPA. Implement ở infrastructure.
 */
public interface OrderBookRepository {

    /** Load sổ theo cặp (vd BTC/VND) — kèm buy/sell đang chờ. */
    OrderBook findByTradingPair(TradingPair pair);

    /** Lưu sổ sau add/remove lệnh (sync list chờ xuống DB). */
    void save(OrderBook orderBook);
}
