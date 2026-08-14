package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountJpaEntity toEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getAccountId());
        entity.setStatus(account.getStatus().name());

        // Sync legacy columns với available VND (nếu có) để DB cũ không null.
        long vndAvailable = account.getAvailable("VND").getAmount();
        entity.setBalanceAmount(vndAvailable);
        entity.setBalanceCurrency("VND");

        for (Balance balance : account.getHoldings().values()) {
            AccountBalanceJpaEntity row = new AccountBalanceJpaEntity();
            row.setAccount(entity);
            row.setCurrency(balance.getCurrency());
            row.setAvailableAmount(balance.getAvailable());
            row.setLockedAmount(balance.getLocked());
            row.setDeleted(false);
            entity.getBalances().add(row);
        }
        return entity;
    }

    public Account toDomain(AccountJpaEntity entity) {
        Map<String, Balance> holdings = new LinkedHashMap<>();

        if (entity.getBalances() != null && !entity.getBalances().isEmpty()) {
            for (AccountBalanceJpaEntity row : entity.getBalances()) {
                if (row.isDeleted()) {
                    continue;
                }
                holdings.put(
                        row.getCurrency(),
                        new Balance(row.getCurrency(), row.getAvailableAmount(), row.getLockedAmount())
                );
            }
        } else if (entity.getBalanceCurrency() != null && entity.getBalanceAmount() != null) {
            // Fallback DB chưa migrate sang account_balances
            holdings.put(
                    entity.getBalanceCurrency(),
                    new Balance(entity.getBalanceCurrency(), entity.getBalanceAmount(), 0)
            );
        }

        return new Account(entity.getId(), AccountStatus.valueOf(entity.getStatus()), holdings);
    }
}
