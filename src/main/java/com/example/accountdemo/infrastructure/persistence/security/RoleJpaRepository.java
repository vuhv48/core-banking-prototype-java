package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA cho {@link RoleJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> CRUD/lookup role trong lớp persistence security.
 */
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {
    Optional<RoleJpaEntity> findByName(String name);
}
