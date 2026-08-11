package com.example.accountdemo.domain.exchange.event;

import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.TradingPair;
import java.time.Instant;

/**
 * Domain Event — thông báo "vừa khớp xong một trade" (immutable, có timestamp).
 *
 * <p>Phân loại DDD:
 * <ul>
 *   <li>Không phải Aggregate — ghi nhận sự kiện đã xảy ra, không phải entity gốc</li>
 *   <li>Không phải Event Sourcing — state chính vẫn lưu qua JPA ({@code orders}, {@code order_books})</li>
 *   <li>Listener (infra) có thể log, gửi Kafka, settle wallet… sau này</li>
 * </ul>
 */
public final class TradeExecutedEvent {

    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final TradingPair tradingPair;
    private final Quantity quantity;
    private final Price price;
    private final Instant occurredAt;

    public TradeExecutedEvent(
            String tradeId,
            String buyOrderId,
            String sellOrderId,
            TradingPair tradingPair,
            Quantity quantity,
            Price price,
            Instant occurredAt
    ) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.tradingPair = tradingPair;
        this.quantity = quantity;
        this.price = price;
        this.occurredAt = occurredAt;
    }

    public String getTradeId() {
        return tradeId;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public TradingPair getTradingPair() {
        return tradingPair;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Price getPrice() {
        return price;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

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
