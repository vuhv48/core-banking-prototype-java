package com.example.accountdemo.domain.exchange;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bản ghi trade đã khớp — dùng để persist lịch sử.
 */
@Getter
@RequiredArgsConstructor
public final class ExecutedTrade {

    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final String buyerAccountId;
    private final String sellerAccountId;
    private final TradingPair tradingPair;
    private final Quantity quantity;
    private final Price price;
}
