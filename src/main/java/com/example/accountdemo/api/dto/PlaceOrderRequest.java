package com.example.accountdemo.api.dto;

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
