package com.example.accountdemo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Mapped superclass: soft-delete + audit timestamps/user.
 *
 * <p><b>Vì sao cần class này:</b> mọi JPA entity dùng chung cột deleted/created/updated — tránh lặp.
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String updatedBy;
}
