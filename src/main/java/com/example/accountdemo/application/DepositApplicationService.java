package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.Money;
import org.springframework.stereotype.Service;

/**
 * Application Service — điều phối use case nạp tiền.
 * KHÔNG chứa business logic; chỉ gọi domain và repository.
 */
@Service
public class DepositApplicationService {

    private final AccountRepository accountRepository;

    public DepositApplicationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Use case: nạp tiền vào tài khoản.
     * Các bước (chỉ điều phối, không có if business rule):
     * 1. Tạo Money từ amount + currency.
     * 2. Account account = accountRepository.findById(accountId).
     * 3. account.deposit(money) — business rule nằm trong Account.
     * 4. accountRepository.save(account).
     */
    public void deposit(String accountId, long amount, String currency) {
        Money money = new Money(amount, currency);
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản: " + accountId);
        }
        account.deposit(money);
        accountRepository.save(account);
    }
}
