package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data JPA cho {@link ResourceJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> load toàn bộ rule API→permission lúc startup vào registry.
 */
public interface ResourceJpaRepository extends JpaRepository<ResourceJpaEntity, Long> {

    /** Rules enabled + permission (JOIN FETCH) cho ApiPermissionRuleRegistry. */
    @Query("""
            SELECT r FROM ResourceJpaEntity r
            JOIN FETCH r.permission p
            WHERE r.deleted = false
              AND r.enabled = true
              AND p.deleted = false
            """)
    List<ResourceJpaEntity> findAllEnabledWithPermission();
}
