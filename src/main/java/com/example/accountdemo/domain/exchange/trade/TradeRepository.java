package com.example.accountdemo.domain.exchange.trade;

import com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade;

public interface TradeRepository {

    void save(ExecutedTrade trade);
}
