package com.example.accountdemo.domain.account.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object — một khoản tiền (số + currency).
 *
 * <p>Amount dùng {@link BigDecimal} để hỗ trợ số thập phân (vd 1.1 BTC).
 * Immutable. Hai Money khác currency không cộng/trừ được.
 */
@Getter
public final class Money {

    public static final int SCALE = 8;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currency;

    /** Tạo khoản tiền; currency bắt buộc (amount có thể 0 khi biểu diễn số dư rỗng). */
    public Money(BigDecimal amount, String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency không được null hoặc rỗng");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount không được null");
        }
        this.amount = normalize(amount);
        this.currency = currency;
    }

    public Money(long amount, String currency) {
        this(BigDecimal.valueOf(amount), currency);
    }

    public Money(String amount, String currency) {
        this(new BigDecimal(amount), currency);
    }

    /** Cộng cùng currency — trả Money mới (immutable). */
    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /** Trừ cùng currency — trả Money mới; caller tự kiểm invariant không âm nếu cần. */
    public Money subtract(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /** True nếu amount &lt; 0. */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /** True nếu amount = 0. */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /** So sánh lớn hơn cùng currency. */
    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING).stripTrailingZeros();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }
}
