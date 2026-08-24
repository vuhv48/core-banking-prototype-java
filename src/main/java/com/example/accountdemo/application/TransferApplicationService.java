package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.Money;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final AccountRepository accountRepository;
    private final OwnershipChecker ownershipGuard;

    @Transactional
    public void transfer(String username, String fromAccountId, String toAccountId, BigDecimal amount, String currency) {
        if (fromAccountId != null && fromAccountId.equals(toAccountId)) {
            throw new DomainException(ErrorStatus.INVALID_ARGUMENT, "Không thể chuyển sang chính tài khoản nguồn");
        }
        ownershipGuard.requireAccountAccess(username, fromAccountId);
        Account fromAccount = accountRepository.findById(fromAccountId);
        Account toAccount = accountRepository.findById(toAccountId);
        if (fromAccount == null || toAccount == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND);
        }
        Money money = new Money(amount, currency);
        fromAccount.withdraw(money);
        toAccount.deposit(money);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }
}
