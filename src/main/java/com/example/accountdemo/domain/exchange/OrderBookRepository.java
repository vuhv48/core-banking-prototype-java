package com.example.accountdemo.domain.exchange;

public interface OrderBookRepository {

    OrderBook findByTradingPair(TradingPair pair);

    void save(OrderBook orderBook);
}
