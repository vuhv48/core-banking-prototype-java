package com.example.accountdemo.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * Tạo/parse JWT access token và hash refresh token.
 *
 * <p><b>Vì sao cần class này:</b> một chỗ quản lý secret, TTL, claim permissions và SHA-256 refresh.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTokenMs;
    private final long refreshTokenMs;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-ms:900000}") long accessTokenMs,
            @Value("${security.jwt.refresh-token-ms:604800000}") long refreshTokenMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMs = accessTokenMs;
        this.refreshTokenMs = refreshTokenMs;
    }

    /** Tạo access token JWT chứa username và danh sách quyền. */
    public String generateAccessToken(String username, List<String> permissions) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("permissions", permissions)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenMs))
                .signWith(signingKey)
                .compact();
    }

    /** Tạo refresh token ngẫu nhiên (UUID-based, không phải JWT). */
    public String generateRefreshToken() {
        return java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public long getAccessTokenMs() {
        return accessTokenMs;
    }

    public long getRefreshTokenMs() {
        return refreshTokenMs;
    }

    /** TTL access token (giây) — trả về client trong login/refresh response. */
    public long getAccessExpiresInSeconds() {
        return accessTokenMs / 1000;
    }

    /** TTL refresh token (giây). */
    public long getRefreshExpiresInSeconds() {
        return refreshTokenMs / 1000;
    }

    /** Parse và validate access token; ném JwtException nếu không hợp lệ. */
    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Token còn hạn và chữ ký hợp lệ? */
    public boolean isTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Lấy username (sub) từ access token. */
    public String getUsernameFromToken(String token) {
        return parseAccessToken(token).getSubject();
    }

    /** Lấy claim permissions từ access token. */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        return (List<String>) parseAccessToken(token).get("permissions");
    }

    /** SHA-256 hash của refresh token để lưu vào DB. */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
