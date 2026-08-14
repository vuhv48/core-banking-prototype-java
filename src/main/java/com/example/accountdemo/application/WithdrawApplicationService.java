package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Money;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * Application Service — điều phối use case rút tiền.
 * <p><b>Vì sao cần class này:</b> nối API với domain Account.withdraw; rule số dư / đóng băng
 * nằm trong Aggregate, service chỉ điều phối persistence.
 */
@Service
@RequiredArgsConstructor
public class WithdrawApplicationService {

    private final AccountRepository accountRepository;

    /**
     * Rút amount/currency khỏi tài khoản; giảm available nếu đủ điều kiện.
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
