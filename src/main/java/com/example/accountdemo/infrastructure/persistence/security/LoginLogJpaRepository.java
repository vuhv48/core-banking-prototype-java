package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA cho {@link LoginLogJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> lưu audit login từ LoginLogService.
 */
public interface LoginLogJpaRepository extends JpaRepository<LoginLogJpaEntity, Long> {
}
