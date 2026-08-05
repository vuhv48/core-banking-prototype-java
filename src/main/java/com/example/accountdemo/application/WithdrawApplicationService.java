package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.Money;
import org.springframework.stereotype.Service;

/**
 * Application Service — điều phối use case rút tiền.
 * KHÔNG chứa business logic; chỉ gọi domain và repository.
 */
@Service
public class WithdrawApplicationService {

    private final AccountRepository accountRepository;

    public WithdrawApplicationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Use case: rút tiền từ tài khoản.
     * Các bước (chỉ điều phối, không có if business rule):
     * 1. Tạo Money từ amount + currency.
     * 2. Account account = accountRepository.findById(accountId).
     * 3. account.withdraw(money) — business rule nằm trong Account.
     * 4. accountRepository.save(account).
     * Nếu account không tồn tại: xử lý ở findById hoặc throw ở đây (không phải business rule).
     */
    public void withdraw(String accountId, long amount, String currency) {
        Money money = new Money(amount, currency);

        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản: " + accountId);
        }

        account.withdraw(money);
        accountRepository.save(account);
    }
}
