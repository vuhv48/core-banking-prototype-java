package com.example.accountdemo.infrastructure.persistence.exchange;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA cho {@link OrderJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> CRUD + query theo cặp tiền cho OrderBook adapter.
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    /** Lệnh thuộc một cặp tiền (để ghép OrderBook). */
    List<OrderJpaEntity> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}
