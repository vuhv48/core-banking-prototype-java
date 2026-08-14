package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA cho {@link UserJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> load user kèm roles/permissions cho login và JWT claims.
 */
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

    /** Username đã tồn tại chưa. */
    boolean existsByUsername(String username);

    /** Email đã tồn tại chưa (đăng ký / validation). */
    boolean existsByEmail(String email);
}
