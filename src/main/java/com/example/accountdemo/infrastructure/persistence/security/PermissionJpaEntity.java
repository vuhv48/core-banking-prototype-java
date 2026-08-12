package com.example.accountdemo.infrastructure.persistence.security;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bảng permissions – mỗi record là một quyền cụ thể, ví dụ ORDER_PLACE, ORDER_CANCEL.
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

    /** HTTP method API yêu cầu quyền này: GET, POST, DELETE, hoặc * (mọi method). */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /** Ant path pattern, ví dụ /api/orders hoặc /api/orders/** */
    @Column(name = "path_pattern", length = 255)
    private String pathPattern;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<RoleJpaEntity> roles = new LinkedHashSet<>();
}
