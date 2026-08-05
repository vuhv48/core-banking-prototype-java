package com.example.accountdemo.domain.account;

public interface AccountRepository {

    Account findById(String accountId);

    void save(Account account);
}
