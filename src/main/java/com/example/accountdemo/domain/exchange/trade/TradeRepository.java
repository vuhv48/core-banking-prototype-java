package com.example.accountdemo.domain.exchange.trade;

import com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade;

/**
 * Port (Repository) — lưu {@link ExecutedTrade} (lịch sử khớp).
 *
 * <p><b>Vì sao cần:</b> insert khi settle thành công; không dùng để tính số dư khả dụng.
 * Domain không phụ thuộc JPA.
 */
public interface TradeRepository {

    /** Insert một lần khớp đã settle — audit / lịch sử. */
    void save(ExecutedTrade trade);
}
