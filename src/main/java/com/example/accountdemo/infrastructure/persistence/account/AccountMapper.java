package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountStatus;
import com.example.accountdemo.domain.account.Money;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountJpaEntity toEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getAccountId());
        entity.setBalanceAmount(account.getBalance().getAmount());
        entity.setBalanceCurrency(account.getBalance().getCurrency());
        entity.setStatus(account.getStatus().name());
        return entity;
    }

    public Account toDomain(AccountJpaEntity entity) {
        Money balance = new Money(entity.getBalanceAmount(), entity.getBalanceCurrency());
        AccountStatus status = AccountStatus.valueOf(entity.getStatus());
        return new Account(entity.getId(), balance, status);
    }
}
