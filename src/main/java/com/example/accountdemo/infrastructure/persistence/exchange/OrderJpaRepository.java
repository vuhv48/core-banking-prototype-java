package com.example.accountdemo.infrastructure.persistence.exchange;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA cho {@link OrderJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> CRUD + query theo cặp tiền cho OrderBook adapter.
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    /** Lệnh thuộc một cặp tiền (để ghép OrderBook). */
    List<OrderJpaEntity> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);

    List<OrderJpaEntity> findByAccountIdAndDeletedFalse(String accountId);

    @Query("""
            select o from OrderJpaEntity o
            where o.deleted = false
              and (:accountId = '' or lower(o.accountId) like :accountId)
              and (:orderId = '' or lower(o.id) like :orderId)
            """)
    Page<OrderJpaEntity> search(
            @Param("accountId") String accountId,
            @Param("orderId") String orderId,
            Pageable pageable
    );
}
