package com.example.accountdemo.domain.exchange.order.model;

/**
 * Enum trạng thái {@link Order}.
 *
 * <p>{@code PENDING}: chưa khớp. {@code PARTIALLY_FILLED}: khớp một phần, còn treo.
 * <p>{@code FILLED} / {@code CANCELLED}: kết thúc ({@link #isFinal()}) — không match/cancel tiếp.
 */
public enum OrderStatus {
    PENDING,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED;

    public boolean isFinal() {
        return this == FILLED || this == CANCELLED;
    }
}
