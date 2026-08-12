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
 * Utility tạo và kiểm tra JWT access token, và hash refresh token.
 *
 * Access token chứa:
 *   - sub: username
 *   - permissions: danh sách tên quyền hiệu lực
 *
 * Refresh token gốc chỉ tồn tại trong response; DB chỉ lưu SHA-256 hash.
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

    public long getRefreshTokenMs() {
        return refreshTokenMs;
    }

    /** Parse và validate access token; ném JwtException nếu không hợp lệ. */
    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseAccessToken(token).getSubject();
    }

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
