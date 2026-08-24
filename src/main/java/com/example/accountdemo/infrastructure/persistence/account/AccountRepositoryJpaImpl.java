package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountPage;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Balance;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Adapter triển khai {@code AccountRepository} bằng Spring Data JPA.
 *
 * <p><b>Vì sao cần class này:</b> domain chỉ biết port; class này sync holdings ↔ bảng account_balances.
 */
@Repository
@RequiredArgsConstructor
public class AccountRepositoryJpaImpl implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountMapper accountMapper;

    /** Tìm ví theo id (bỏ soft-deleted); null nếu không có. */
    @Override
    @Transactional(readOnly = true)
    public Account findById(String accountId) {
        return accountJpaRepository.findById(accountId)
                .filter(entity -> !entity.isDeleted())
                .map(accountMapper::toDomain)
                .orElse(null);
    }

    /** Insert hoặc cập nhật status + sync balances. */
    @Override
    @Transactional
    public void save(Account account) {
        LocalDateTime now = LocalDateTime.now();
        Optional<AccountJpaEntity> existing = accountJpaRepository.findById(account.getAccountId());

        if (existing.isPresent()) {
            AccountJpaEntity current = existing.get();
            current.setStatus(account.getStatus().name());
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

    @Override
    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountJpaRepository.findAll()
                .stream().filter(entity -> !entity.isDeleted())
                .map(accountMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountPage findPage(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<AccountJpaEntity> result = accountJpaRepository.findByDeletedFalse(pageable);
        List<Account> content = result.getContent().stream()
                .map(accountMapper::toDomain)
                .toList();
        return new AccountPage(content, page, size, result.getTotalElements());
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
