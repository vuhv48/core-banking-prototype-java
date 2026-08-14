package com.example.accountdemo.domain.exchange.orderbook;

import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.shared.TradingPair;

public interface OrderBookRepository {

    OrderBook findByTradingPair(TradingPair pair);

    void save(OrderBook orderBook);
}
