package com.example.accountdemo.domain.exchange.shared;

import com.example.accountdemo.domain.account.model.Money;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object — giá giao dịch (&gt; 0), hỗ trợ thập phân.
 */
@Getter
public final class Price {

    private final BigDecimal value;

    public Price(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Giá không được null");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá phải lớn hơn 0");
        }
        this.value = Money.normalize(value);
    }

    public Price(long value) {
        this(BigDecimal.valueOf(value));
    }

    public Price(String value) {
        this(new BigDecimal(value));
    }

    public boolean isGreaterThan(Price other) {
        ensureNotNull(other);
        return this.value.compareTo(other.value) > 0;
    }

    public boolean isLessThan(Price other) {
        ensureNotNull(other);
        return this.value.compareTo(other.value) < 0;
    }

    private void ensureNotNull(Price other) {
        if (other == null) {
            throw new IllegalArgumentException("other không được null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Price price)) return false;
        return value.compareTo(price.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
