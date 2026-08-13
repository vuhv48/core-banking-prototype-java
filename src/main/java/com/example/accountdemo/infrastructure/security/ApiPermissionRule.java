package com.example.accountdemo.infrastructure.security;

import org.springframework.util.AntPathMatcher;

/**
 * Một rule map API (method + path) sang tên permission.
 * Load từ bảng resources.
 */
record ApiPermissionRule(String httpMethod, String pathPattern, String permissionName) {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    boolean matches(String requestMethod, String requestPath) {
        if (!"*".equals(httpMethod) && !httpMethod.equalsIgnoreCase(requestMethod)) {
            return false;
        }
        return PATH_MATCHER.match(pathPattern, requestPath);
    }

    /** Pattern càng dài/cụ thể thì ưu tiên cao hơn. */
    int specificity() {
        int score = pathPattern.length();
        if (!pathPattern.contains("*")) {
            score += 100;
        }
        return score;
    }
}
