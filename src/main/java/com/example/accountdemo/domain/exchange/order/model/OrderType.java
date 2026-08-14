package com.example.accountdemo.domain.exchange.order.model;

/**
 * Enum kiểu lệnh (gắn {@link Order}).
 *
 * <p><b>Vì sao cần:</b> phân biệt lệnh có giá treo sổ (LIMIT) với lệnh khớp ngay /
 * hủy phần dư (MARKET) — matching và lock ví xử lý khác nhau.
 */
public enum OrderType {
    /** Khớp giá thị trường; phần chưa khớp không treo sổ (cancel). */
    MARKET,
    /** Phải có giá; phần chưa khớp treo trên OrderBook. */
    LIMIT
}
