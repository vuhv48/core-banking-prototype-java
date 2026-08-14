package com.example.accountdemo.infrastructure.persistence.exchange;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA cho {@link OrderBookJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> tìm sổ lệnh theo base/quote; adapter domain dùng interface này.
 */
public interface OrderBookJpaRepository extends JpaRepository<OrderBookJpaEntity, String> {

    /** Tìm sổ lệnh theo cặp base/quote. */
    Optional<OrderBookJpaEntity> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}
