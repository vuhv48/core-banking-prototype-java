package com.example.accountdemo.domain.exchange.order.model;

/**
 * Enum phía lệnh — mua hoặc bán (gắn {@link Order}).
 *
 * <p>{@code BUY}: lock quote (VND). {@code SELL}: lock base (BTC).
 */
public enum OrderSide {
    BUY,
    SELL
}
