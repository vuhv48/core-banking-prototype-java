package com.example.accountdemo.api.auth;

import com.example.accountdemo.api.auth.dto.LoginRequest;
import com.example.accountdemo.api.auth.dto.LoginResponse;
import com.example.accountdemo.api.auth.dto.LogoutResponse;
import com.example.accountdemo.api.auth.dto.RefreshRequest;
import com.example.accountdemo.api.auth.dto.RefreshTokenResponse;
import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.infrastructure.persistence.security.RefreshTokenJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.RefreshTokenJpaRepository;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaRepository;
import com.example.accountdemo.infrastructure.security.JwtUtil;
import com.example.accountdemo.infrastructure.security.LoginLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoint đăng nhập và làm mới token.
 * Không yêu cầu JWT – đã khai báo permitAll trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserJpaRepository userJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final LoginLogService loginLogService;

    /**
     * POST /api/auth/login
     * Body: { "username": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String username = auth.getName();
        List<String> permissions = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtUtil.generateAccessToken(username, permissions);

        String rawRefresh = jwtUtil.generateRefreshToken();
        String tokenHash = jwtUtil.hashToken(rawRefresh);

        UserJpaEntity user = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND));

        RefreshTokenJpaEntity tokenEntity = RefreshTokenJpaEntity.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenMs()))
                .revoked(false)
                .build();
        tokenEntity.setDeleted(false);
        tokenEntity.setCreatedAt(LocalDateTime.now());
        tokenEntity.setUpdatedAt(LocalDateTime.now());
        refreshTokenJpaRepository.save(tokenEntity);

        loginLogService.recordSuccessfulLogin(user.getId(), username, httpRequest);

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtUtil.getAccessExpiresInSeconds(),
                jwtUtil.getRefreshExpiresInSeconds(),
                permissions
        ));
    }

    /**
     * POST /api/auth/refresh
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshRequest request) {
        String tokenHash = jwtUtil.hashToken(request.refreshToken());

        RefreshTokenJpaEntity tokenEntity = refreshTokenJpaRepository
                .findByTokenHash(tokenHash)
                .orElse(null);

        if (tokenEntity == null
                || tokenEntity.isRevoked()
                || tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new DomainException(ErrorStatus.REFRESH_TOKEN_INVALID);
        }

        tokenEntity.setRevoked(true);
        tokenEntity.setUpdatedAt(LocalDateTime.now());
        refreshTokenJpaRepository.save(tokenEntity);

        UserJpaEntity user = tokenEntity.getUser();
        List<String> permissions = userJpaRepository.findByUsername(user.getUsername())
                .map(u -> {
                    List<String> perms = u.getRoles().stream()
                            .flatMap(r -> r.getPermissions().stream())
                            .map(p -> p.getName())
                            .toList();
                    List<String> direct = u.getDirectPermissions().stream()
                            .map(p -> p.getName())
                            .toList();
                    return Stream.concat(perms.stream(), direct.stream())
                            .distinct()
                            .toList();
                })
                .orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), permissions);

        String newRaw = jwtUtil.generateRefreshToken();
        String newHash = jwtUtil.hashToken(newRaw);

        RefreshTokenJpaEntity newToken = RefreshTokenJpaEntity.builder()
                .user(user)
                .tokenHash(newHash)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenMs()))
                .revoked(false)
                .build();
        newToken.setDeleted(false);
        newToken.setCreatedAt(LocalDateTime.now());
        newToken.setUpdatedAt(LocalDateTime.now());
        refreshTokenJpaRepository.save(newToken);

        return ResponseEntity.ok(new RefreshTokenResponse(
                newAccessToken,
                newRaw,
                "Bearer",
                jwtUtil.getAccessExpiresInSeconds(),
                jwtUtil.getRefreshExpiresInSeconds()
        ));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(Authentication authentication) {
        if (authentication == null) {
            throw new DomainException(ErrorStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();
        userJpaRepository.findByUsername(username).ifPresent(user ->
                refreshTokenJpaRepository.revokeAllByUserId(user.getId())
        );

        return ResponseEntity.ok(new LogoutResponse("Đăng xuất thành công"));
    }
}
