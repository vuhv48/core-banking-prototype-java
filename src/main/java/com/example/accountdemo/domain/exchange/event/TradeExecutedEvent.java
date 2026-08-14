package com.example.accountdemo.domain.exchange.event;

import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Domain Event — “vừa khớp xong” (để log / Kafka).
 *
 * <p><b>Vì sao cần class này:</b> thông báo side-effect sau settle mà không gắn logic
 * vào Aggregate. Payload đủ để subscriber biết trade nào, cặp nào, giá/qty.
 *
 * <pre>
 * tradeId     = TRD-001
 * buyOrderId  = ORD-BUY-001
 * sellOrderId = ORD-SELL-001
 * tradingPair = BTC/VND
 * quantity    = 1
 * price       = 60_000_000
 * occurredAt  = 2026-08-14T03:25:00Z
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public final class TradeExecutedEvent {

    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final TradingPair tradingPair;
    private final Quantity quantity;
    private final Price price;
    private final Instant occurredAt;

    /** Chuỗi đọc được khi log event (không dùng cho persistence). */
    @Override
    public String toString() {
        return "TradeExecutedEvent{"
                + "tradeId='" + tradeId + '\''
                + ", buyOrderId='" + buyOrderId + '\''
                + ", sellOrderId='" + sellOrderId + '\''
                + ", tradingPair=" + tradingPair
                + ", quantity=" + quantity.getValue()
                + ", price=" + price.getValue()
                + ", occurredAt=" + occurredAt
                + '}';
    }
}
