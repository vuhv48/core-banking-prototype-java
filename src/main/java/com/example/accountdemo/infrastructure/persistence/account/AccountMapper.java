package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Map qua lại domain {@code Account} và {@link AccountJpaEntity} (+ balances).
 *
 * <p><b>Vì sao cần class này:</b> giữ domain sạch khỏi JPA annotation; adapter chỉ gọi mapper.
 */
@Component
public class AccountMapper {

    /** Domain → JPA (kèm rows account_balances). */
    public AccountJpaEntity toEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getAccountId());
        entity.setStatus(account.getStatus().name());

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

    /** JPA → Domain từ account_balances. */
    public Account toDomain(AccountJpaEntity entity) {
        Map<String, Balance> holdings = new LinkedHashMap<>();

        if (entity.getBalances() != null) {
            for (AccountBalanceJpaEntity row : entity.getBalances()) {
                if (row.isDeleted()) {
                    continue;
                }
                holdings.put(
                        row.getCurrency(),
                        new Balance(row.getCurrency(), row.getAvailableAmount(), row.getLockedAmount())
                );
            }
        }

        return new Account(entity.getId(), AccountStatus.valueOf(entity.getStatus()), holdings);
    }
}
