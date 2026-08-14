package com.example.accountdemo.domain.exchange.matching;

import lombok.Getter;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;

/**
 * Value Object — một bản ghi nhát khớp trên RAM (chưa lưu DB).
 *
 * <pre>
 * buyOrderId      = ORD-BUY-001
 * sellOrderId     = ORD-SELL-001
 * matchedQuantity = 1
 * matchedPrice    = 60_000_000
 * </pre>
 *
 * Persist = {@link com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade}.
 */
@Getter
public final class Trade {

    /** Id lệnh mua đã khớp. */
    private final String buyOrderId;
    /** Id lệnh bán đã khớp. */
    private final String sellOrderId;
    /** Số lượng khớp trong lần này (min remaining hai bên). */
    private final Quantity matchedQuantity;
    /** Giá khớp — lấy theo lệnh đã nằm sẵn trên sổ (maker). */
    private final Price matchedPrice;

    public Trade(
            String buyOrderId,
            String sellOrderId,
            Quantity matchedQuantity,
            Price matchedPrice
    ) {
        if (buyOrderId == null || buyOrderId.isBlank()) {
            throw new IllegalArgumentException("buyOrderId không được null hoặc rỗng");
        }
        if (sellOrderId == null || sellOrderId.isBlank()) {
            throw new IllegalArgumentException("sellOrderId không được null hoặc rỗng");
        }
        if (matchedQuantity == null || matchedQuantity.getValue() <= 0) {
            throw new IllegalArgumentException("matchedQuantity phải lớn hơn 0");
        }
        if (matchedPrice == null) {
            throw new IllegalArgumentException("matchedPrice không được null");
        }
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.matchedQuantity = matchedQuantity;
        this.matchedPrice = matchedPrice;
    }
}
