package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Money;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * Application Service — điều phối use case nạp tiền.
 * <p><b>Vì sao cần class này:</b> nối API với domain Account.deposit; giữ rule nghiệp vụ
 * trong Aggregate, service chỉ load → gọi → save.
 */
@Service
@RequiredArgsConstructor
public class DepositApplicationService {

    private final AccountRepository accountRepository;

    /**
     * Nạp amount/currency vào tài khoản; tăng available của đồng tương ứng.
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
