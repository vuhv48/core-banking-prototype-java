package com.example.accountdemo.infrastructure.persistence.security;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bảng permissions – quyền nghiệp vụ (ORDER_PLACE, ORDER_READ...).
 * Mapping API nằm ở bảng {@code resources}, không gắn path vào đây.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên quyền duy nhất, ví dụ ORDER_PLACE. */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Mô tả ngắn để dễ quản lý. */
    @Column(length = 255)
    private String description;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<RoleJpaEntity> roles = new LinkedHashSet<>();
}
