package com.example.accountdemo.domain.account;

import com.example.accountdemo.domain.account.model.Account;

/**
 * Port (Repository) — persistence của aggregate {@link Account}.
 *
 * <p>Thuộc domain: khai báo "cần load/save Account". Implement JPA ở infrastructure.
 * <p>Không chứa business rule — rule nằm trong {@link Account}.
 */
public interface AccountRepository {

    Account findById(String accountId);

    void save(Account account);
}
