package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {
    Optional<PermissionJpaEntity> findByName(String name);

    /** Permissions có mapping API (http_method + path_pattern) để AuthorizationFilter đọc. */
    @Query("""
            SELECT p FROM PermissionJpaEntity p
            WHERE p.pathPattern IS NOT NULL
              AND p.httpMethod IS NOT NULL
              AND p.deleted = false
            """)
    List<PermissionJpaEntity> findAllApiMappings();
}
