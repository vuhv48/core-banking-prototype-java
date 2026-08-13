package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResourceJpaRepository extends JpaRepository<ResourceJpaEntity, Long> {

    @Query("""
            SELECT r FROM ResourceJpaEntity r
            JOIN FETCH r.permission p
            WHERE r.deleted = false
              AND r.enabled = true
              AND p.deleted = false
            """)
    List<ResourceJpaEntity> findAllEnabledWithPermission();
}
