package com.example.accountdemo.domain.exchange.shared;

import lombok.Getter;

/**
 * Value Object — giá giao dịch (số nguyên &gt; 0).
 *
 * <p><b>Vì sao cần class này:</b> không để {@code long} trần làm giá — bắt buộc &gt; 0
 * và so sánh có nghĩa (best bid/ask, price compatible khi match).
 *
 * <pre>
 * value = 60_000_000
 * </pre>
 */
@Getter
public final class Price {

    private final long value;

    /** Tạo giá; phải &gt; 0 (không cho giá 0 / âm). */
    public Price(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Giá phải lớn hơn 0");
        }
        this.value = value;
    }

    /** True nếu this &gt; other — dùng tìm best bid (giá mua cao nhất). */
    public boolean isGreaterThan(Price other) {
        ensureNotNull(other);
        return this.value > other.value;
    }

    /** True nếu this &lt; other — dùng tìm best ask (giá bán thấp nhất). */
    public boolean isLessThan(Price other) {
        ensureNotNull(other);
        return this.value < other.value;
    }

    private void ensureNotNull(Price other) {
        if (other == null) {
            throw new IllegalArgumentException("other không được null");
        }
    }
}
