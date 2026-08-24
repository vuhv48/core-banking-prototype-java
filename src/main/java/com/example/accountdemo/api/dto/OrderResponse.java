package com.example.accountdemo.api.dto;

import com.example.accountdemo.domain.exchange.order.model.Order;

import java.math.BigDecimal;

/**
 * Response chi tiết lệnh (sau GET/cancel).
 *
 * <p><b>Vì sao cần class này:</b> serialize Order domain sang JSON phẳng (enum → string, value object → số).
 */
public record OrderResponse(
        String orderId,
        String accountId,
        String side,
        String orderType,
        String tradingPair,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal filledQuantity,
        String status,
        String lockedCurrency,
        BigDecimal lockedAmountRemaining
) {
    /** Map domain Order → DTO. */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getAccountId(),
                order.getSide().name(),
                order.getOrderType().name(),
                order.getTradingPair().toString(),
                order.getQuantity().getValue(),
                order.getPrice() != null ? order.getPrice().getValue() : null,
                order.getFilledQuantity().getValue(),
                order.getStatus().name(),
                order.getLockedCurrency(),
                order.getLockedAmountRemaining()
        );
    }
}
