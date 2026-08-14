package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.infrastructure.persistence.security.LoginLogJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.LoginLogJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Ghi audit log khi user đăng nhập thành công.
 *
 * <p><b>Vì sao cần class này:</b> AuthController không viết JPA trực tiếp cho login_logs.
 */
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogJpaRepository loginLogJpaRepository;

    /** Persist một dòng login_logs (IP, User-Agent). */
    public void recordSuccessfulLogin(Long userId, String username, HttpServletRequest request) {
        LoginLogJpaEntity log = LoginLogJpaEntity.builder()
                .userId(userId)
                .username(username)
                .ipAddress(resolveClientIp(request))
                .userAgent(truncate(request.getHeader("User-Agent"), 512))
                .loggedInAt(LocalDateTime.now())
                .build();

        loginLogJpaRepository.save(log);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
