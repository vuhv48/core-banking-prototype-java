package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountStatus;
import com.example.accountdemo.domain.account.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    void toEntity_shouldMapDomainFields() {
        Account account = new Account("ACC-001", new Money(100_000, "VND"), AccountStatus.ACTIVE);

        AccountJpaEntity entity = accountMapper.toEntity(account);

        assertEquals("ACC-001", entity.getId());
        assertEquals(100_000, entity.getBalanceAmount());
        assertEquals("VND", entity.getBalanceCurrency());
        assertEquals("ACTIVE", entity.getStatus());
    }

    @Test
    void toDomain_shouldMapEntityFields() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId("ACC-002");
        entity.setBalanceAmount(200_000);
        entity.setBalanceCurrency("VND");
        entity.setStatus("FROZEN");

        Account account = accountMapper.toDomain(entity);

        assertEquals("ACC-002", account.getAccountId());
        assertEquals(200_000, account.getBalance().getAmount());
        assertEquals("VND", account.getBalance().getCurrency());
        assertEquals(AccountStatus.FROZEN, account.getStatus());
    }
}
