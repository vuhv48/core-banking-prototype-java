package com.example.accountdemo.infrastructure.persistence.exchange;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    List<OrderJpaEntity> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}
