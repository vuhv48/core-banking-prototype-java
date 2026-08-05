package com.example.accountdemo.infrastructure.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, String> {
}
