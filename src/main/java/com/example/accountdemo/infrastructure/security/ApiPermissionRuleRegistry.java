package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.infrastructure.persistence.security.PermissionJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.PermissionJpaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cache rules đọc từ bảng permissions lúc startup.
 * Thêm/sửa mapping API chỉ cần UPDATE permissions trong DB (restart app để reload).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionRuleRegistry {

    private final PermissionJpaRepository permissionJpaRepository;

    private List<ApiPermissionRule> rules = List.of();

    @PostConstruct
    public void loadRules() {
        rules = permissionJpaRepository.findAllApiMappings().stream()
                .map(this::toRule)
                .sorted(Comparator.comparingInt(ApiPermissionRule::specificity).reversed())
                .toList();

        log.info("Loaded {} API permission rules from DB:", rules.size());
        rules.forEach(rule ->
                log.info("  {} {} -> {}", rule.httpMethod(), rule.pathPattern(), rule.permissionName())
        );
    }

    public Optional<String> findRequiredPermission(String httpMethod, String requestPath) {
        return rules.stream()
                .filter(rule -> rule.matches(httpMethod, requestPath))
                .findFirst()
                .map(ApiPermissionRule::permissionName);
    }

    private ApiPermissionRule toRule(PermissionJpaEntity entity) {
        return new ApiPermissionRule(
                entity.getHttpMethod(),
                entity.getPathPattern(),
                entity.getName()
        );
    }
}
