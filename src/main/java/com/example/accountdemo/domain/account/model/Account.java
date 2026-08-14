package com.example.accountdemo.domain.account.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Aggregate Root — ví / tài khoản của một người dùng.
 *
 * <p><b>Vì sao cần class này:</b> mọi thay đổi số dư (nạp, rút, treo lệnh, tất toán khớp)
 * phải đi qua đây để giữ invariant: không âm, FROZEN không debit, available/locked tách bạch.
 * Application/API không được {@code set} thẳng map holdings.
 *
 * <pre>
 * accountId = ACC-001
 * status    = ACTIVE
 * holdings  = {
 *   VND: available=10_000_000, locked=0
 *   BTC: available=5,          locked=0
 * }
 * </pre>
 */
@Getter
public class Account {

    private final String accountId;
    private AccountStatus status;
    @Getter(AccessLevel.NONE)
    private final Map<String, Balance> holdings;

    /**
     * Tạo / khôi phục ví đầy đủ (nhiều currency).
     * Dùng khi load từ DB hoặc seed multi-currency.
     */
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

    /**
     * Tạo ví một currency, locked = 0.
     * Giữ tương thích API/test cũ chỉ biết một số dư (vd chỉ VND).
     */
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

    /**
     * Nạp tiền vào available.
     * Không đụng locked — tiền mới vào là dùng được ngay (kể cả account FROZEN vẫn cho nạp).
     */
    public void deposit(Money amount) {
        requirePositiveMoney(amount);
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.credit(amount.getAmount()));
    }

    /**
     * Rút tiền từ available.
     * Cần ACTIVE; không được đụng phần đang locked trên lệnh.
     */
    public void withdraw(Money amount) {
        requirePositiveMoney(amount);
        ensureActiveForDebit();
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.debitAvailable(amount.getAmount()));
    }

    /**
     * Treo tiền khi đặt lệnh: available → locked.
     * Tránh trừ thẳng lúc place — cancel mới trả được đúng phần chưa khớp.
     */
    public void reserve(Money amount) {
        requirePositiveMoney(amount);
        ensureActiveForDebit();
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.reserve(amount.getAmount()));
    }

    /**
     * Trả tiền treo về available (hủy lệnh / phần lock thừa khi khớp giá tốt hơn).
     */
    public void release(Money amount) {
        requirePositiveMoney(amount);
        Balance current = requireHolding(amount.getCurrency());
        holdings.put(amount.getCurrency(), current.release(amount.getAmount()));
    }

    /**
     * Chi phần đã treo khi khớp: locked giảm, không về available.
     * Buyer mất VND / seller mất BTC đã reserve — tiền đã chuyển cho đối phương qua {@link #credit}.
     */
    public void consumeLocked(Money amount) {
        requirePositiveMoney(amount);
        Balance current = requireHolding(amount.getCurrency());
        holdings.put(amount.getCurrency(), current.consumeLocked(amount.getAmount()));
    }

    /**
     * Cộng available khi nhận tài sản sau khớp (buyer nhận BTC, seller nhận VND).
     * Khác deposit: đây là tất toán trade, không phải user nạp tiền.
     */
    public void credit(Money amount) {
        requirePositiveMoney(amount);
        Balance current = holdings.getOrDefault(amount.getCurrency(), Balance.zero(amount.getCurrency()));
        holdings.put(amount.getCurrency(), current.credit(amount.getAmount()));
    }

    /** Số dư dùng được của một currency (0 nếu chưa có dòng). */
    public Money getAvailable(String currency) {
        Balance balance = holdings.get(currency);
        if (balance == null) {
            return new Money(0, currency);
        }
        return balance.toAvailableMoney();
    }

    /** Số dư đang treo trên lệnh của một currency (0 nếu chưa có dòng). */
    public Money getLocked(String currency) {
        Balance balance = holdings.get(currency);
        if (balance == null) {
            return new Money(0, currency);
        }
        return new Money(balance.getLocked(), currency);
    }

    /**
     * Available VND nếu có, không thì currency đầu tiên.
     * Giữ tương thích chỗ cũ gọi {@code getBalance()} thay vì {@code getAvailable("VND")}.
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

    /**
     * Toàn bộ số dư theo currency — trả map không sửa được để ngoài không phá invariant.
     */
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
