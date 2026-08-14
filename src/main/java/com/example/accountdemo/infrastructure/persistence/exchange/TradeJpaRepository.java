package com.example.accountdemo.infrastructure.persistence.exchange;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA cho {@link TradeJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> CRUD thấp tầng cho adapter TradeRepository.
 */
public interface TradeJpaRepository extends JpaRepository<TradeJpaEntity, String> {
}
