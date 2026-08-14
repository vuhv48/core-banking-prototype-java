package com.example.accountdemo.domain.exchange.order.model;

/**
 * Enum trạng thái {@link Order}.
 *
 * <p><b>Vì sao cần:</b> biết lệnh còn khớp/hủy được không; {@link #isFinal()} chặn
 * match/cancel trên lệnh đã FILLED hoặc CANCELLED.
 */
public enum OrderStatus {
    /** Chưa khớp phần nào. */
    PENDING,
    /** Đã khớp một phần, còn treo trên sổ. */
    PARTIALLY_FILLED,
    /** Khớp hết — không match/cancel tiếp. */
    FILLED,
    /** Đã hủy — không match/cancel tiếp. */
    CANCELLED;

    /** True nếu lệnh đã kết thúc (FILLED hoặc CANCELLED). */
    public boolean isFinal() {
        return this == FILLED || this == CANCELLED;
    }
}
