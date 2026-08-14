package com.example.accountdemo.domain.exchange.shared;

import lombok.Getter;

/**
 * Value Object — một bản ghi số lượng (≥ 0).
 *
 * <pre>
 * value = 1
 * </pre>
 */
@Getter
public final class Quantity {

    private final long value;

    /**
     * Tạo số lượng mới.
     * - Gán value vào field.
     * - Validate: value phải >= 0 → throw IllegalArgumentException nếu < 0.
     * Ví dụ: new Quantity(100) hợp lệ, new Quantity(-1) không hợp lệ.
     */
    public Quantity(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity value cannot be negative");
        }
        this.value = value;
    }

    private void ensureNotNull(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("quantity không được null");
        }
    }

    /**
     * Cộng hai số lượng.
     * - other không được null → throw IllegalArgumentException.
     * - Trả về Quantity mới với value = this.value + other.value.
     * Ví dụ: 100 + 50 = 150.
     */
    public Quantity plus(Quantity other) {
        ensureNotNull(other);
        return new Quantity(this.value + other.value);
    }

    /**
     * Trừ hai số lượng.
     * - other không được null → throw IllegalArgumentException.
     * - Trả về Quantity mới với value = this.value - other.value.
     * - Kết quả có thể bằng 0; nếu âm thì throw (tùy thiết kế).
     * Ví dụ: 100 - 30 = 70.
     */
    public Quantity minus(Quantity other) {
        ensureNotNull(other);
        if (this.value < other.value) {
            throw new IllegalArgumentException("Số lượng không đủ để trừ (kết quả âm)");
        }
        return new Quantity(this.value - other.value);
    }

    /**
     * So sánh this >= other.
     * - other không được null → throw IllegalArgumentException.
     * - Trả về true nếu this.value >= other.value.
     * Dùng trong Order.match() để kiểm tra executedQuantity không vượt remaining.
     */
    public boolean isGreaterThanOrEqual(Quantity other) {
        ensureNotNull(other);
        return this.value >= other.value;
    }

    /**
     * Kiểm tra số lượng bằng 0.
     * - Trả về true nếu value == 0.
     * Dùng để biết order đã khớp hết chưa.
     */
    public boolean isZero() {
        return this.value == 0;
    }
}
