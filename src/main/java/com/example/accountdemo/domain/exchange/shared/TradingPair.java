package com.example.accountdemo.domain.exchange.shared;

import lombok.Getter;

/**
 * Value Object đại diện cặp giao dịch, ví dụ BTC/VND, ETH/USDT.
 *
 * Ví dụ: BTC/VND nghĩa là mua/bán BTC, giá tính bằng VND.
 * - baseCurrency = "BTC"  (tài sản được giao dịch)
 * - quoteCurrency = "VND" (đơn vị tiền dùng để định giá)
 */
@Getter
public final class TradingPair {

    private final String baseCurrency;
    private final String quoteCurrency;

    /**
     * Tạo cặp giao dịch mới.
     * - Gán baseCurrency và quoteCurrency vào field.
     * - Validate: cả hai không được null hoặc rỗng → throw IllegalArgumentException.
     * - baseCurrency và quoteCurrency phải khác nhau.
     * Ví dụ: new TradingPair("BTC", "VND") → BTC/VND.
     */
    public TradingPair(String baseCurrency, String quoteCurrency) {
        if (baseCurrency == null || baseCurrency.isBlank()) {
            throw new IllegalArgumentException("baseCurrency không được null hoặc rỗng");
        }
        if (quoteCurrency == null || quoteCurrency.isBlank()) {
            throw new IllegalArgumentException("quoteCurrency không được null hoặc rỗng");
        }
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            throw new IllegalArgumentException("baseCurrency và quoteCurrency phải khác nhau");
        }
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
    }

    /** Format chuỗi dạng "BTC/VND". */
    @Override
    public String toString() {
        return baseCurrency + "/" + quoteCurrency;
    }
}
