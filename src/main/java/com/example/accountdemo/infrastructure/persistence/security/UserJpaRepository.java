package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /**
     * Load user cùng với roles và directPermissions trong một query để tránh N+1.
     * Dùng khi build SecurityContext trong JwtAuthFilter.
     */
    @EntityGraph(attributePaths = {
            "roles",
            "roles.permissions",
            "directPermissions"
    })
    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
