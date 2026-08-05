package com.example.accountdemo.domain.account;

/**
 * Aggregate Root — tài khoản ngân hàng.
 * Tự bảo vệ business rule: không rút khi FROZEN, không rút quá số dư.
 */
public class Account {

    private String accountId;
    private Money balance;
    private AccountStatus status;

    public Account(String accountId, Money balance, AccountStatus status) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId không được null hoặc rỗng");
        }
        if (balance == null) {
            throw new IllegalArgumentException("balance không được null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status không được null");
        }
        this.accountId = accountId;
        this.balance = balance;
        this.status = status;
    }

    public void withdraw(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount không được null");
        }
        if (amount.isNegative() || amount.getAmount() == 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        }
        if (status == AccountStatus.FROZEN) {
            throw new IllegalStateException("Tài khoản đã bị khóa, không được rút tiền");
        }

        Money newBalance = balance.subtract(amount);
        if (newBalance.isNegative()) {
            throw new IllegalArgumentException("Tài khoản không đủ tiền");
        }
        balance = newBalance;
    }

    public void deposit(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount không được null");
        }
        if (amount.isNegative() || amount.getAmount() == 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        balance = balance.add(amount);
    }

    public String getAccountId() {
        return accountId;
    }

    public Money getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
