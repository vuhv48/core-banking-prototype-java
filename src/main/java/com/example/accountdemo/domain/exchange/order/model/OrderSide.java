package com.example.accountdemo.domain.exchange.order.model;

/**
 * Enum phía lệnh — mua hoặc bán (gắn {@link Order}).
 *
 * <p><b>Vì sao cần:</b> quyết định lock currency nào (quote vs base) và phía nào
 * trên sổ (bid/ask) khi matching.
 */
public enum OrderSide {
    /** Mua base — lock quote (vd VND). */
    BUY,
    /** Bán base — lock base (vd BTC). */
    SELL
}
