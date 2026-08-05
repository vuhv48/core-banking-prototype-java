package com.example.accountdemo.domain.account;

/**
 * Value Object đại diện một khoản tiền: số tiền + loại tiền tệ.
 * Immutable — mỗi phép tính trả về Money mới, không sửa object cũ.
 */
public final class Money {

    private final long amount;
    private final String currency;

    public Money(long amount, String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency không được null hoặc rỗng");
        }
        this.amount = amount;
        this.currency = currency;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money subtract(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount - other.amount, this.currency);
    }

    public boolean isNegative() {
        return amount < 0;
    }

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
