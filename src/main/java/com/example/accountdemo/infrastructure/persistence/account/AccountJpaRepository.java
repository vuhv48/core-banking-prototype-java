package com.example.accountdemo.infrastructure.persistence.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA cho {@link AccountJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> CRUD thấp tầng; adapter {@code AccountRepositoryJpaImpl} mới expose port domain.
 */
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, String> {

    Page<AccountJpaEntity> findByDeletedFalse(Pageable pageable);
}
