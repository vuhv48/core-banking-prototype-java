package com.example.accountdemo.infrastructure.persistence.account;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryJpaImpl implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountMapper accountMapper;

    public AccountRepositoryJpaImpl(AccountJpaRepository accountJpaRepository, AccountMapper accountMapper) {
        this.accountJpaRepository = accountJpaRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public Account findById(String accountId) {
        return accountJpaRepository.findById(accountId)
                .filter(entity -> !entity.isDeleted())
                .map(accountMapper::toDomain)
                .orElse(null);
    }

    @Override
    public void save(Account account) {
        AccountJpaEntity entity = accountMapper.toEntity(account);
        LocalDateTime now = LocalDateTime.now();

        Optional<AccountJpaEntity> existing = accountJpaRepository.findById(account.getAccountId());
        if (existing.isPresent()) {
            AccountJpaEntity current = existing.get();
            entity.setDeleted(current.isDeleted());
            entity.setCreatedAt(current.getCreatedAt());
            entity.setCreatedBy(current.getCreatedBy());
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(current.getUpdatedBy());
        } else {
            entity.setDeleted(false);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
        }

        accountJpaRepository.save(entity);
    }
}
