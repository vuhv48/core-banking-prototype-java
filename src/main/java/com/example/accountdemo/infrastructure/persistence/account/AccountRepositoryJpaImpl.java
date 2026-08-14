package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Balance;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryJpaImpl implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional(readOnly = true)
    public Account findById(String accountId) {
        return accountJpaRepository.findById(accountId)
                .filter(entity -> !entity.isDeleted())
                .map(accountMapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional
    public void save(Account account) {
        LocalDateTime now = LocalDateTime.now();
        Optional<AccountJpaEntity> existing = accountJpaRepository.findById(account.getAccountId());

        if (existing.isPresent()) {
            AccountJpaEntity current = existing.get();
            current.setStatus(account.getStatus().name());
            current.setBalanceAmount(account.getAvailable("VND").getAmount());
            current.setBalanceCurrency("VND");
            current.setUpdatedAt(now);
            syncBalances(current, account, now);
            accountJpaRepository.save(current);
            return;
        }

        AccountJpaEntity entity = accountMapper.toEntity(account);
        entity.setDeleted(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        for (AccountBalanceJpaEntity row : entity.getBalances()) {
            row.setDeleted(false);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
        }
        accountJpaRepository.save(entity);
    }

    private void syncBalances(AccountJpaEntity entity, Account account, LocalDateTime now) {
        Map<String, AccountBalanceJpaEntity> byCurrency = new HashMap<>();
        for (AccountBalanceJpaEntity row : entity.getBalances()) {
            byCurrency.put(row.getCurrency(), row);
        }

        for (Balance balance : account.getHoldings().values()) {
            AccountBalanceJpaEntity row = byCurrency.remove(balance.getCurrency());
            if (row == null) {
                row = new AccountBalanceJpaEntity();
                row.setAccount(entity);
                row.setCurrency(balance.getCurrency());
                row.setDeleted(false);
                row.setCreatedAt(now);
                entity.getBalances().add(row);
            }
            row.setAvailableAmount(balance.getAvailable());
            row.setLockedAmount(balance.getLocked());
            row.setUpdatedAt(now);
        }

        // Currency không còn trong domain → xóa
        Iterator<AccountBalanceJpaEntity> it = entity.getBalances().iterator();
        while (it.hasNext()) {
            AccountBalanceJpaEntity row = it.next();
            if (byCurrency.containsKey(row.getCurrency())) {
                it.remove();
            }
        }
    }
}
