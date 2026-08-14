package com.example.accountdemo.api.auth.dto;

import java.util.List;

/**
 * Response sau login: access/refresh token + TTL + permissions hiệu lực.
 *
 * <p><b>Vì sao cần class này:</b> client cần payload chuẩn để gọi API bảo vệ và refresh sau này.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds,
        List<String> permissions
) {
}
