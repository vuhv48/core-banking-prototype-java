package com.example.accountdemo.domain.exchange.order.model;

public enum OrderStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED;

    public boolean isFinal() {
        return this == FILLED || this == CANCELLED;
    }
}
