package com.example.accountdemo.infrastructure.persistence.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log mỗi lần user đăng nhập thành công.
 * Tương tự admin_login_logs trong các hệ thống khác, nhưng ghi cho mọi user.
 */
@Entity
@Table(name = "login_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;
}
