package com.example.accountdemo.domain.exchange.shared;

import lombok.Getter;

/**
 * Value Object — số lượng mua/bán (≥ 0).
 *
 * <p><b>Vì sao cần class này:</b> cộng/trừ filled/remaining an toàn; không cho âm;
 * {@code isZero} / {@code isGreaterThanOrEqual} phục vụ rule khớp lệnh.
 *
 * <pre>
 * value = 1
 * </pre>
 */
@Getter
public final class Quantity {

    private final long value;

    /** Tạo số lượng; cho phép 0 (filled ban đầu / remaining hết), không cho âm. */
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

    /** Cộng hai số lượng — trả Quantity mới (vd tăng filled sau match). */
    public Quantity plus(Quantity other) {
        ensureNotNull(other);
        return new Quantity(this.value + other.value);
    }

    /** Trừ hai số lượng — không cho kết quả âm (vd remaining = total − filled). */
    public Quantity minus(Quantity other) {
        ensureNotNull(other);
        if (this.value < other.value) {
            throw new IllegalArgumentException("Số lượng không đủ để trừ (kết quả âm)");
        }
        return new Quantity(this.value - other.value);
    }

    /** True nếu this ≥ other — chặn khớp vượt remaining trong {@code Order.match}. */
    public boolean isGreaterThanOrEqual(Quantity other) {
        ensureNotNull(other);
        return this.value >= other.value;
    }

    /** True nếu = 0 — biết lệnh đã khớp hết / không còn gì để match. */
    public boolean isZero() {
        return this.value == 0;
    }
}
