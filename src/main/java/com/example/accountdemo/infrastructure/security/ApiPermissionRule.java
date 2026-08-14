package com.example.accountdemo.infrastructure.security;

import org.springframework.util.AntPathMatcher;

/**
 * Một rule map API (method + path Ant) sang tên permission — load từ bảng resources.
 *
 * <p><b>Vì sao cần class này:</b> model trong RAM cho AuthorizationFilter match request nhanh.
 */
record ApiPermissionRule(String httpMethod, String pathPattern, String permissionName) {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** Request có khớp method + Ant path pattern không. */
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
