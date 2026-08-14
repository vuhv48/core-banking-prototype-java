package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA cho {@link PermissionJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> CRUD/lookup permission khi seed hoặc gán role.
 */
public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {
    Optional<PermissionJpaEntity> findByName(String name);
}
