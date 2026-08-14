package com.example.accountdemo.domain.exchange.shared;

import lombok.Getter;

/**
 * Value Object — một bản ghi cặp giao dịch.
 *
 * <pre>
 * baseCurrency  = BTC
 * quoteCurrency = VND
 * </pre>
 *
 * toString = "BTC/VND". Hai currency phải khác nhau.
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
