package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FreezeAccountApplicationService {

    private final AccountRepository accountRepository;
    private final OwnershipChecker ownershipGuard;

    @Transactional
    public void freeze(String username, String accountId) {
        Account account = loadOwnedAccount(username, accountId);
        account.freeze();
        accountRepository.save(account);
    }

    @Transactional
    public void unfreeze(String username, String accountId) {
        Account account = loadOwnedAccount(username, accountId);
        account.unfreeze();
        accountRepository.save(account);
    }

    private Account loadOwnedAccount(String username, String accountId) {
        ownershipGuard.requireAccountAccess(username, accountId);
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND);
        }
        return account;
    }
}
