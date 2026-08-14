package com.example.accountdemo.domain.exchange.trade;

import com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade;

/**
 * Port (Repository) — lưu {@link ExecutedTrade} (lịch sử khớp).
 *
 * <p>Chỉ insert khi settle thành công. Không dùng để tính số dư khả dụng.
 */
public interface TradeRepository {

    void save(ExecutedTrade trade);
}
