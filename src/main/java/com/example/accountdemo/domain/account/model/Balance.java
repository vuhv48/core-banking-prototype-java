package com.example.accountdemo.domain.account.model;

import lombok.Getter;

/**
 * Value Object — một dòng số dư theo currency bên trong {@link Account}.
 *
 * <p><b>Vì sao cần:</b> tách {@code available} / {@code locked} để đặt lệnh không trừ thẳng,
 * cancel/settle mới biết trả hay chi bao nhiêu. Immutable — mỗi thao tác trả Balance mới.
 *
 * <pre>
 * currency  = VND
 * available = 10_000_000
 * locked    = 0
 * </pre>
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

    /** Dòng số dư rỗng khi account chưa từng có currency này. */
    public static Balance zero(String currency) {
        return new Balance(currency, 0, 0);
    }

    /** available → locked (đặt lệnh). */
    public Balance reserve(long amount) {
        requirePositive(amount);
        if (available < amount) {
            throw new IllegalArgumentException("Số dư khả dụng không đủ để giữ: " + currency);
        }
        return new Balance(currency, available - amount, locked + amount);
    }

    /** locked → available (hủy / thừa). */
    public Balance release(long amount) {
        requirePositive(amount);
        if (locked < amount) {
            throw new IllegalArgumentException("Locked không đủ để giải phóng: " + currency);
        }
        return new Balance(currency, available + amount, locked - amount);
    }

    /** locked giảm khi đã chi cho đối phương (khớp lệnh). */
    public Balance consumeLocked(long amount) {
        requirePositive(amount);
        if (locked < amount) {
            throw new IllegalArgumentException("Locked không đủ để tất toán: " + currency);
        }
        return new Balance(currency, available, locked - amount);
    }

    /** Cộng available (nạp hoặc nhận sau khớp). */
    public Balance credit(long amount) {
        requirePositive(amount);
        return new Balance(currency, available + amount, locked);
    }

    /** Trừ available (rút tiền). */
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
