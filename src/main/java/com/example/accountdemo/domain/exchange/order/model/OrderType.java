package com.example.accountdemo.domain.exchange.order.model;

/**
 * Enum kiểu lệnh (gắn {@link Order}).
 *
 * <p>{@code LIMIT}: phải có giá; phần chưa khớp treo sổ.
 * <p>{@code MARKET}: khớp giá thị trường; BUY MARKET hiện reject ở application (chưa biết lock bao nhiêu VND).
 */
public enum OrderType {
    MARKET,
    LIMIT
}
