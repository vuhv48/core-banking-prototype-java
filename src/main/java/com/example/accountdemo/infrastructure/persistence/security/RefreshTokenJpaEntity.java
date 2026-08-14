package com.example.accountdemo.infrastructure.persistence.security;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity bảng {@code refresh_tokens} — lưu hash refresh token.
 *
 * <p><b>Vì sao cần class này:</b> cấp lại access token không cần login; hash SHA-256 tránh lộ token gốc.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    /** SHA-256 hash của token gốc. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    /** true = đã bị thu hồi (logout hoặc rotate). */
    @Column(nullable = false)
    private boolean revoked = false;
}
