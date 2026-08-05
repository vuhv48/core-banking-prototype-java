package com.example.accountdemo.infrastructure.persistence.exchange;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderBookJpaRepository extends JpaRepository<OrderBookJpaEntity, String> {

    Optional<OrderBookJpaEntity> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}
