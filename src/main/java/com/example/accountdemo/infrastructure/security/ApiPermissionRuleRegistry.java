package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.infrastructure.persistence.security.ResourceJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.ResourceJpaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cache rules đọc từ bảng resources lúc startup.
 * Một permission có thể map nhiều resource (nhiều path).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPermissionRuleRegistry {

    private final ResourceJpaRepository resourceJpaRepository;

    private List<ApiPermissionRule> rules = List.of();

    @PostConstruct
    public void loadRules() {
        rules = resourceJpaRepository.findAllEnabledWithPermission().stream()
                .map(this::toRule)
                .sorted(Comparator.comparingInt(ApiPermissionRule::specificity).reversed())
                .toList();

        log.info("Loaded {} API resource rules from DB:", rules.size());
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

    private ApiPermissionRule toRule(ResourceJpaEntity entity) {
        return new ApiPermissionRule(
                entity.getHttpMethod(),
                entity.getPathPattern(),
                entity.getPermission().getName()
        );
    }
}
