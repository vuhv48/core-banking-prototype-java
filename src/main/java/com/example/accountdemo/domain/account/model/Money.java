package com.example.accountdemo.domain.account.model;

import lombok.Getter;

/**
 * Value Object — một khoản tiền (số + currency).
 *
 * <p><b>Vì sao cần class này:</b> tránh thao tác {@code long} trần (dễ nhầm currency, dễ âm).
 * Mọi cộng/trừ phải cùng currency — invariant nằm ở đây, không ở Application.
 *
 * <pre>
 * amount   = 10_000_000
 * currency = VND
 * </pre>
 *
 * Immutable. Hai Money khác currency không cộng/trừ được.
 */
@Getter
public final class Money {

    private final long amount;
    private final String currency;

    /** Tạo khoản tiền; currency bắt buộc (amount có thể 0 khi biểu diễn số dư rỗng). */
    public Money(long amount, String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency không được null hoặc rỗng");
        }
        this.amount = amount;
        this.currency = currency;
    }

    /** Cộng cùng currency — trả Money mới (immutable). */
    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount + other.amount, this.currency);
    }

    /** Trừ cùng currency — trả Money mới; caller tự kiểm invariant không âm nếu cần. */
    public Money subtract(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount - other.amount, this.currency);
    }

    /** True nếu amount &lt; 0 — dùng chặn deposit/withdraw/reserve số âm. */
    public boolean isNegative() {
        return amount < 0;
    }

    /** So sánh lớn hơn cùng currency (vd đủ tiền để rút chưa). */
    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount > other.amount;
    }

    private void ensureSameCurrency(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("other không được null");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Không thể thao tác với hai loại tiền khác nhau: "
                            + this.currency + " và " + other.currency
            );
        }
    }
}
