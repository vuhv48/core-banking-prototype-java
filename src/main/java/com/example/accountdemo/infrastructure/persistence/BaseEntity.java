package com.example.accountdemo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Data;

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
