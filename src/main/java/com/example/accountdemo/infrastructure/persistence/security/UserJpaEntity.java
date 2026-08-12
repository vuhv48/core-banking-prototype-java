package com.example.accountdemo.infrastructure.persistence.security;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bảng users – lưu thông tin đăng nhập.
 * Tách khỏi Account domain để đảm bảo tính độc lập.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /** Mật khẩu đã hash bằng BCrypt. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, length = 150)
    private String email;

    /** Liên kết tuỳ chọn với Account domain (nullable), ví dụ ACC-001. */
    @Column(name = "account_id", length = 50)
    private String accountId;

    /** true = tài khoản chưa bị khoá. */
    @Column(nullable = false)
    private boolean enabled = true;

    /** user_roles: user này thuộc những role nào. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleJpaEntity> roles = new LinkedHashSet<>();

    /** user_permissions: quyền gán trực tiếp cho user (không qua role). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionJpaEntity> directPermissions = new LinkedHashSet<>();
}
