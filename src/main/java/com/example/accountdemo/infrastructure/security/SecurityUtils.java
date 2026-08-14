package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper lấy username hiện tại từ SecurityContext.
 *
 * <p><b>Vì sao cần class này:</b> controller/application lấy identity thống nhất, tránh lặp cast Authentication.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Username đang đăng nhập; ném UNAUTHORIZED nếu chưa authenticate. */
    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new DomainException(ErrorStatus.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String username) {
            return username;
        }
        return principal.toString();
    }
}
