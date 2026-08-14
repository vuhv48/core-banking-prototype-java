package com.example.accountdemo.domain.exchange;

import lombok.Getter;

/**
 * Value Object đại diện giá giao dịch.
 * Immutable — mỗi phép so sánh/tính toán không sửa object cũ.
 */
@Getter
public final class Price {

    private final long value;

    /**
     * Tạo giá mới.
     * - Gán value vào field.
     * - Validate: value phải > 0 → throw IllegalArgumentException nếu <= 0.
     * Ví dụ: new Price(50000) hợp lệ, new Price(0) hoặc new Price(-1) không hợp lệ.
     */
    public Price(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Giá phải lớn hơn 0");
        }
        this.value = value;
    }

    /**
     * So sánh this có lớn hơn other không.
     * - other không được null → throw IllegalArgumentException.
     * - Trả về true nếu this.value > other.value.
     * Dùng trong OrderBook để tìm best bid (giá mua cao nhất).
     */
    public boolean isGreaterThan(Price other) {
        ensureNotNull(other);
        return this.value > other.value;
    }

    /**
     * So sánh this có nhỏ hơn other không.
     * - other không được null → throw IllegalArgumentException.
     * - Trả về true nếu this.value < other.value.
     * Dùng trong OrderBook để tìm best ask (giá bán thấp nhất).
     */
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
