package com.example.accountdemo.domain.account.model;

import lombok.Getter;

/**
 * Value Object — một bản ghi số dư theo currency (trong Account).
 *
 * <pre>
 * currency  = VND
 * available = 10_000_000
 * locked    = 0
 * </pre>
 *
 * Immutable: reserve/release/consumeLocked/credit trả Balance mới. available ≥ 0, locked ≥ 0.
 */
@Getter
public final class Balance {

    private final String currency;
    private final long available;
    private final long locked;

    public Balance(String currency, long available, long locked) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency không được null hoặc rỗng");
        }
        if (available < 0) {
            throw new IllegalArgumentException("available không được âm");
        }
        if (locked < 0) {
            throw new IllegalArgumentException("locked không được âm");
        }
        this.currency = currency;
        this.available = available;
        this.locked = locked;
    }

    public static Balance zero(String currency) {
        return new Balance(currency, 0, 0);
    }

    public Balance reserve(long amount) {
        requirePositive(amount);
        if (available < amount) {
            throw new IllegalArgumentException("Số dư khả dụng không đủ để giữ: " + currency);
        }
        return new Balance(currency, available - amount, locked + amount);
    }

    public Balance release(long amount) {
        requirePositive(amount);
        if (locked < amount) {
            throw new IllegalArgumentException("Locked không đủ để giải phóng: " + currency);
        }
        return new Balance(currency, available + amount, locked - amount);
    }

    public Balance consumeLocked(long amount) {
        requirePositive(amount);
        if (locked < amount) {
            throw new IllegalArgumentException("Locked không đủ để tất toán: " + currency);
        }
        return new Balance(currency, available, locked - amount);
    }

    public Balance credit(long amount) {
        requirePositive(amount);
        return new Balance(currency, available + amount, locked);
    }

    public Balance debitAvailable(long amount) {
        requirePositive(amount);
        if (available < amount) {
            throw new IllegalArgumentException("Số dư khả dụng không đủ: " + currency);
        }
        return new Balance(currency, available - amount, locked);
    }

    public Money toAvailableMoney() {
        return new Money(available, currency);
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount phải lớn hơn 0");
        }
    }
}
