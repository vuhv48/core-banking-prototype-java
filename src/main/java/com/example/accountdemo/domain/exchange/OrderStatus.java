package com.example.accountdemo.domain.exchange;

public enum OrderStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED;

    public boolean isFinal() {
        return this == FILLED || this == CANCELLED;
    }
}
