package com.example.accountdemo.api.dto;

/**
 * Body đặt lệnh: side, type, cặp, quantity, price (null nếu MARKET).
 *
 * <p><b>Vì sao cần class này:</b> nhận input HTTP dạng string/long trước khi map sang value object domain.
 */
public record PlaceOrderRequest(
        String accountId,
        String side,
        String orderType,
        String baseCurrency,
        String quoteCurrency,
        long quantity,
        Long price
) {
}
