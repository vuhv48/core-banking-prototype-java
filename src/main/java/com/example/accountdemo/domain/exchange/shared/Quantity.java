package com.example.accountdemo.domain.exchange.shared;

import com.example.accountdemo.domain.account.model.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object — số lượng mua/bán (≥ 0), hỗ trợ thập phân.
 */
@Getter
public final class Quantity {

    private final BigDecimal value;

    public Quantity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Quantity value không được null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity value cannot be negative");
        }
        this.value = Money.normalize(value);
    }

    public Quantity(long value) {
        this(BigDecimal.valueOf(value));
    }

    public Quantity(String value) {
        this(new BigDecimal(value));
    }

    private void ensureNotNull(Quantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("quantity không được null");
        }
    }

    public Quantity plus(Quantity other) {
        ensureNotNull(other);
        return new Quantity(this.value.add(other.value));
    }

    public Quantity minus(Quantity other) {
        ensureNotNull(other);
        if (this.value.compareTo(other.value) < 0) {
            throw new IllegalArgumentException("Số lượng không đủ để trừ (kết quả âm)");
        }
        return new Quantity(this.value.subtract(other.value));
    }

    public boolean isGreaterThanOrEqual(Quantity other) {
        ensureNotNull(other);
        return this.value.compareTo(other.value) >= 0;
    }

    public boolean isZero() {
        return this.value.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity quantity)) return false;
        return value.compareTo(quantity.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
