package com.example.accountdemo.domain.account.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Aggregate Root — ví / tài khoản.
 * Mỗi currency có available (xài được) và locked (đang treo trên lệnh).
 */
@Getter
public class Account {

    private final String accountId;
    private AccountStatus status;
    @Getter(AccessLevel.NONE)
    private final Map<String, Balance> holdings;

    public Account(String accountId, AccountStatus status, Map<String, Balance> holdings) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId không được null hoặc rỗng");
        }
        if (status == null) {
            throw new IllegalArgumentException("status không được null");
        }
        this.accountId = accountId;
        this.status = status;
        this.holdings = new LinkedHashMap<>();
        if (holdings != null) {
            holdings.forEach((currency, balance) -> this.holdings.put(currency, balance));
        }
    }

    /** Tạo account một currency, locked = 0 (tương thích deposit/withdraw cũ). */
    public Account(String accountId, Money availableBalance, AccountStatus status) {
        this(accountId, status, toHoldings(availableBalance));
    }

    private static Map<String, Balance> toHoldings(Money availableBalance) {
        if (availableBalance == null) {
            throw new IllegalArgumentException("balance không được null");
        }
        Map<String, Balance> map = new LinkedHashMap<>();
        map.put(
                availableBalance.getCurrency(),
                new Balance(availableBalance.getCurrency(), availableBalance.getAmount(), 0)
        );
        return map;
    }

    public void deposit(Money amount) {
        requirePositiveMoney(amount);
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.credit(amount.getAmount()));
    }

    public void withdraw(Money amount) {
        requirePositiveMoney(amount);
        ensureActiveForDebit();
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.debitAvailable(amount.getAmount()));
    }

    /** available → locked (đặt lệnh). */
    public void reserve(Money amount) {
        requirePositiveMoney(amount);
        ensureActiveForDebit();
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.reserve(amount.getAmount()));
    }

    /** locked → available (hủy lệnh / thừa). */
    public void release(Money amount) {
        requirePositiveMoney(amount);
        Balance current = requireHolding(amount.getCurrency());
        holdings.put(amount.getCurrency(), current.release(amount.getAmount()));
    }

    /** locked giảm, không về available (đã chi khi khớp). */
    public void consumeLocked(Money amount) {
        requirePositiveMoney(amount);
        Balance current = requireHolding(amount.getCurrency());
        holdings.put(amount.getCurrency(), current.consumeLocked(amount.getAmount()));
    }

    /** Cộng available (nhận khi khớp). */
    public void credit(Money amount) {
        requirePositiveMoney(amount);
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.credit(amount.getAmount()));
    }

    public Money getAvailable(String currency) {
        Balance balance = holdings.get(currency);
        if (balance == null) {
            return new Money(0, currency);
        }
        return balance.toAvailableMoney();
    }

    public Money getLocked(String currency) {
        Balance balance = holdings.get(currency);
        if (balance == null) {
            return new Money(0, currency);
        }
        return new Money(balance.getLocked(), currency);
    }

    /**
     * Tương thích code cũ: available của VND nếu có, không thì currency đầu tiên.
     */
    public Money getBalance() {
        if (holdings.containsKey("VND")) {
            return getAvailable("VND");
        }
        if (holdings.isEmpty()) {
            return new Money(0, "VND");
        }
        Balance first = holdings.values().iterator().next();
        return first.toAvailableMoney();
    }

    public Map<String, Balance> getHoldings() {
        return Collections.unmodifiableMap(holdings);
    }

    private Balance requireHolding(String currency) {
        Balance current = holdings.get(currency);
        if (current == null) {
            throw new IllegalArgumentException("Không có số dư currency: " + currency);
        }
        return current;
    }

    private void ensureActiveForDebit() {
        if (status == AccountStatus.FROZEN) {
            throw new IllegalStateException("Tài khoản đã bị khóa, không được rút/giữ tiền");
        }
    }

    private static void requirePositiveMoney(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount không được null");
        }
        if (amount.isNegative() || amount.getAmount() == 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
    }
}
