package com.example.accountdemo.domain.account.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Value Object — một dòng số dư theo currency bên trong {@link Account}.
 * Immutable — mỗi thao tác trả Balance mới. Amount dùng BigDecimal.
 */
@Getter
public final class Balance {

    private final String currency;
    private final BigDecimal available;
    private final BigDecimal locked;

    public Balance(String currency, BigDecimal available, BigDecimal locked) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency không được null hoặc rỗng");
        }
        if (available == null || locked == null) {
            throw new IllegalArgumentException("available/locked không được null");
        }
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("available không được âm");
        }
        if (locked.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("locked không được âm");
        }
        this.currency = currency;
        this.available = Money.normalize(available);
        this.locked = Money.normalize(locked);
    }

    public Balance(String currency, long available, long locked) {
        this(currency, BigDecimal.valueOf(available), BigDecimal.valueOf(locked));
    }

    public static Balance zero(String currency) {
        return new Balance(currency, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Balance reserve(BigDecimal amount) {
        requirePositive(amount);
        if (available.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số dư khả dụng không đủ để giữ: " + currency);
        }
        return new Balance(currency, available.subtract(amount), locked.add(amount));
    }

    public Balance release(BigDecimal amount) {
        requirePositive(amount);
        if (locked.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Locked không đủ để giải phóng: " + currency);
        }
        return new Balance(currency, available.add(amount), locked.subtract(amount));
    }

    public Balance consumeLocked(BigDecimal amount) {
        requirePositive(amount);
        if (locked.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Locked không đủ để tất toán: " + currency);
        }
        return new Balance(currency, available, locked.subtract(amount));
    }

    public Balance credit(BigDecimal amount) {
        requirePositive(amount);
        return new Balance(currency, available.add(amount), locked);
    }

    public Balance debitAvailable(BigDecimal amount) {
        requirePositive(amount);
        if (available.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Số dư khả dụng không đủ: " + currency);
        }
        return new Balance(currency, available.subtract(amount), locked);
    }

    public Money toAvailableMoney() {
        return new Money(available, currency);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount phải lớn hơn 0");
        }
    }
}
