package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogJpaRepository extends JpaRepository<LoginLogJpaEntity, Long> {
}
