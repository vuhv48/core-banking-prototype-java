package com.example.accountdemo.infrastructure.persistence.exchange;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeJpaRepository extends JpaRepository<TradeJpaEntity, String> {
}
