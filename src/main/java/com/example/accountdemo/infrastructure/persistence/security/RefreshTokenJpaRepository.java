package com.example.accountdemo.infrastructure.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Spring Data JPA cho {@link RefreshTokenJpaEntity}.
 *
 * <p><b>Vì sao cần class này:</b> tìm token theo hash và revoke khi logout/refresh rotate.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    /** Tìm refresh token theo SHA-256 hash. */
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    /** Thu hồi tất cả refresh token của một user (logout). */
    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(Long userId);
}
