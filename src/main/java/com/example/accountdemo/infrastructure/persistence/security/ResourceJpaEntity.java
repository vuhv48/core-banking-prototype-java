package com.example.accountdemo.infrastructure.persistence.security;

import com.example.accountdemo.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Bảng resources – map HTTP API (method + path) sang một permission.
 * Một permission có thể gắn nhiều resource.
 */
@Entity
@Table(
        name = "resources",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resources_method_path",
                columnNames = {"http_method", "path_pattern"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên gợi nhớ (tuỳ chọn), ví dụ ORDER_LIST_API. */
    @Column(length = 100)
    private String name;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 255)
    private String pathPattern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionJpaEntity permission;

    @Column(nullable = false)
    private boolean enabled = true;
}
