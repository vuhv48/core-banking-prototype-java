package com.example.accountdemo.api.dto;

import java.math.BigDecimal;

/**
 * Body đặt lệnh: side, type, cặp, quantity, price (null nếu MARKET).
 *
 * <p><b>Vì sao cần class này:</b> nhận input HTTP dạng string/number trước khi map sang value object domain.
 */
public record PlaceOrderRequest(
        String accountId,
        String side,
        String orderType,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal quantity,
        BigDecimal price
) {
}
