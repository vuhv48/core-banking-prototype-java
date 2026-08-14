package com.example.accountdemo.domain.exchange.shared;

import lombok.Getter;

/**
 * Value Object — cặp giao dịch (base/quote).
 *
 * <p><b>Vì sao cần class này:</b> định danh một thị trường (vd BTC/VND); OrderBook
 * và lệnh phải cùng pair. Hai currency khác nhau — tránh "BTC/BTC".
 *
 * <pre>
 * baseCurrency  = BTC
 * quoteCurrency = VND
 * </pre>
 *
 * {@code toString} = "BTC/VND".
 */
@Getter
public final class TradingPair {

    private final String baseCurrency;
    private final String quoteCurrency;

    /** Tạo cặp; base và quote bắt buộc, khác nhau (vd BTC + VND). */
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

    /** Format chuỗi dạng "BTC/VND" — key sổ / hiển thị. */
    @Override
    public String toString() {
        return baseCurrency + "/" + quoteCurrency;
    }
}
