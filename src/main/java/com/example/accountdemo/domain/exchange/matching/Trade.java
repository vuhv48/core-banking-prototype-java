package com.example.accountdemo.domain.exchange.matching;

import lombok.Getter;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;

import java.math.BigDecimal;

/**
 * Value Object — một nhát khớp trên RAM (chưa lưu DB).
 *
 * <p><b>Vì sao cần class này:</b> output tạm của matching — Application dùng để settle ví,
 * persist {@link com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade}, publish event.
 * Không phải entity lịch sử.
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

    /** Tạo nhát khớp; qty &gt; 0, price bắt buộc (giá maker). */
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
        if (matchedQuantity == null || matchedQuantity.getValue().compareTo(BigDecimal.ZERO) <= 0) {
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
