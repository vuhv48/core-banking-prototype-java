package com.example.accountdemo.domain.account;

import com.example.accountdemo.domain.account.model.Account;

public interface AccountRepository {

    Account findById(String accountId);

    void save(Account account);
}
