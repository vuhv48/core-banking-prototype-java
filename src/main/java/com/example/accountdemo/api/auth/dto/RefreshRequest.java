package com.example.accountdemo.api.auth.dto;

/**
 * Body refresh: refresh token gốc (chưa hash).
 *
 * <p><b>Vì sao cần class này:</b> DTO riêng cho /auth/refresh, không tái dùng LoginRequest.
 */
public record RefreshRequest(String refreshToken) {
}
