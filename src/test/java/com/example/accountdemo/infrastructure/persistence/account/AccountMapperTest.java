package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import com.example.accountdemo.domain.account.model.Money;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    void toEntity_shouldMapDomainFields() {
        Account account = new Account("ACC-001", new Money(100_000, "VND"), AccountStatus.ACTIVE);

        AccountJpaEntity entity = accountMapper.toEntity(account);

        assertEquals("ACC-001", entity.getId());
        assertEquals("ACTIVE", entity.getStatus());
        assertEquals(100_000, entity.getBalanceAmount());
        assertEquals("VND", entity.getBalanceCurrency());
        assertEquals(1, entity.getBalances().size());
        assertEquals(100_000, entity.getBalances().get(0).getAvailableAmount());
        assertEquals(0, entity.getBalances().get(0).getLockedAmount());
    }

    @Test
    void toDomain_shouldMapEntityFields() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId("ACC-002");
        entity.setStatus("FROZEN");
        AccountBalanceJpaEntity balance = new AccountBalanceJpaEntity();
        balance.setCurrency("VND");
        balance.setAvailableAmount(200_000);
        balance.setLockedAmount(0);
        balance.setDeleted(false);
        entity.getBalances().add(balance);

        Account account = accountMapper.toDomain(entity);

        assertEquals("ACC-002", account.getAccountId());
        assertEquals(200_000, account.getBalance().getAmount());
        assertEquals(AccountStatus.FROZEN, account.getStatus());
    }

    @Test
    void toDomain_shouldMapMultiCurrency() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId("ACC-001");
        entity.setStatus("ACTIVE");

        AccountBalanceJpaEntity vnd = new AccountBalanceJpaEntity();
        vnd.setCurrency("VND");
        vnd.setAvailableAmount(9_000_000);
        vnd.setLockedAmount(1_000_000);
        vnd.setDeleted(false);

        AccountBalanceJpaEntity btc = new AccountBalanceJpaEntity();
        btc.setCurrency("BTC");
        btc.setAvailableAmount(2);
        btc.setLockedAmount(0);
        btc.setDeleted(false);

        entity.getBalances().add(vnd);
        entity.getBalances().add(btc);

        Account account = accountMapper.toDomain(entity);

        assertEquals(9_000_000, account.getAvailable("VND").getAmount());
        assertEquals(1_000_000, account.getLocked("VND").getAmount());
        assertEquals(2, account.getAvailable("BTC").getAmount());
    }

    @Test
    void roundTrip_multiCurrency() {
        Map<String, Balance> holdings = new LinkedHashMap<>();
        holdings.put("VND", new Balance("VND", 8_000_000, 2_000_000));
        holdings.put("BTC", new Balance("BTC", 3, 1));
        Account original = new Account("ACC-001", AccountStatus.ACTIVE, holdings);

        Account restored = accountMapper.toDomain(accountMapper.toEntity(original));

        assertEquals(8_000_000, restored.getAvailable("VND").getAmount());
        assertEquals(2_000_000, restored.getLocked("VND").getAmount());
        assertEquals(3, restored.getAvailable("BTC").getAmount());
        assertEquals(1, restored.getLocked("BTC").getAmount());
    }
}
