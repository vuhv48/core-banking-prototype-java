package com.example.accountdemo.domain.exchange.trade.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;

/**
 * Entity — một bản ghi lịch sử khớp (bảng trades).
 *
 * <p><b>Vì sao cần class này:</b> audit / đối soát sau settle — khác {@link
 * com.example.accountdemo.domain.exchange.matching.Trade} (VO tạm trên RAM lúc match).
 * Có tradeId + account hai bên để truy vết ví.
 *
 * <pre>
 * tradeId         = TRD-001
 * buyOrderId      = ORD-BUY-001
 * sellOrderId     = ORD-SELL-001
 * buyerAccountId  = ACC-001
 * sellerAccountId = ACC-002
 * tradingPair     = BTC/VND
 * quantity        = 1
 * price           = 60_000_000
 * </pre>
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
