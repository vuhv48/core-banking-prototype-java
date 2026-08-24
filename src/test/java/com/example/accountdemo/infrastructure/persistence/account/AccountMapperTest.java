package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    void toEntity_shouldMapDomainFields() {
        Map<String, Balance> holdings = new LinkedHashMap<>();
        holdings.put("VND", new Balance("VND", 100_000, 0));
        Account account = new Account("ACC-001", AccountStatus.ACTIVE, holdings);

        AccountJpaEntity entity = accountMapper.toEntity(account);

        assertEquals("ACC-001", entity.getId());
        assertEquals("ACTIVE", entity.getStatus());
        assertEquals(1, entity.getBalances().size());
        assertEquals("VND", entity.getBalances().get(0).getCurrency());
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(entity.getBalances().get(0).getAvailableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(entity.getBalances().get(0).getLockedAmount()));
    }

    @Test
    void toDomain_shouldMapEntityFields() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId("ACC-002");
        entity.setStatus("FROZEN");
        AccountBalanceJpaEntity balance = new AccountBalanceJpaEntity();
        balance.setCurrency("VND");
        balance.setAvailableAmount(BigDecimal.valueOf(200_000));
        balance.setLockedAmount(BigDecimal.ZERO);
        balance.setDeleted(false);
        entity.getBalances().add(balance);

        Account account = accountMapper.toDomain(entity);

        assertEquals("ACC-002", account.getAccountId());
        assertEquals(0, BigDecimal.valueOf(200_000).compareTo(account.getAvailable("VND").getAmount()));
        assertEquals(AccountStatus.FROZEN, account.getStatus());
    }

    @Test
    void toDomain_shouldMapMultiCurrency() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId("ACC-001");
        entity.setStatus("ACTIVE");

        AccountBalanceJpaEntity vnd = new AccountBalanceJpaEntity();
        vnd.setCurrency("VND");
        vnd.setAvailableAmount(BigDecimal.valueOf(9_000_000));
        vnd.setLockedAmount(BigDecimal.valueOf(1_000_000));
        vnd.setDeleted(false);

        AccountBalanceJpaEntity btc = new AccountBalanceJpaEntity();
        btc.setCurrency("BTC");
        btc.setAvailableAmount(BigDecimal.valueOf(2));
        btc.setLockedAmount(BigDecimal.ZERO);
        btc.setDeleted(false);

        entity.getBalances().add(vnd);
        entity.getBalances().add(btc);

        Account account = accountMapper.toDomain(entity);

        assertEquals(0, BigDecimal.valueOf(9_000_000).compareTo(account.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(1_000_000).compareTo(account.getLocked("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(account.getAvailable("BTC").getAmount()));
    }

    @Test
    void roundTrip_multiCurrency() {
        Map<String, Balance> holdings = new LinkedHashMap<>();
        holdings.put("VND", new Balance("VND", 8_000_000, 2_000_000));
        holdings.put("BTC", new Balance("BTC", 3, 1));
        Account original = new Account("ACC-001", AccountStatus.ACTIVE, holdings);

        Account restored = accountMapper.toDomain(accountMapper.toEntity(original));

        assertEquals(0, BigDecimal.valueOf(8_000_000).compareTo(restored.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(2_000_000).compareTo(restored.getLocked("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(3).compareTo(restored.getAvailable("BTC").getAmount()));
        assertEquals(0, BigDecimal.valueOf(1).compareTo(restored.getLocked("BTC").getAmount()));
    }
}
