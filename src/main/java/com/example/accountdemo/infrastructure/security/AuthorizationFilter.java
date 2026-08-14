package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.api.common.ErrorStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter 2 – Authorization: kiểm tra permission theo bảng resources.
 *
 * <p><b>Vì sao cần class này:</b> phân quyền tập trung theo DB, không dùng {@code @PreAuthorize};
 * public paths (yaml) được bỏ qua.
 */
@Component
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiPermissionRuleRegistry ruleRegistry;
    private final SecurityProperties securityProperties;
    private final JsonErrorWriter jsonErrorWriter;

    /** Kiểm tra permission theo resources; bỏ qua public path / anonymous. */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        var requiredPermission = ruleRegistry.findRequiredPermission(method, path);
        if (requiredPermission.isEmpty()) {
            jsonErrorWriter.write(
                    request,
                    response,
                    ErrorStatus.FORBIDDEN,
                    "Không có rule quyền cho API này"
            );
            return;
        }

        String permission = requiredPermission.get();
        boolean allowed = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(permission));

        if (!allowed) {
            jsonErrorWriter.write(
                    request,
                    response,
                    ErrorStatus.FORBIDDEN,
                    "Thiếu quyền: " + permission
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        List<String> publicPaths = securityProperties.getPublicPaths();
        if (publicPaths == null) {
            return false;
        }
        return publicPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
