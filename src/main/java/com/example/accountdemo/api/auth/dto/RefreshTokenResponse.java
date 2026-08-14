package com.example.accountdemo.api.auth.dto;

/**
 * Response sau refresh: cặp token mới + TTL.
 *
 * <p><b>Vì sao cần class này:</b> client thay token cũ bằng cặp mới sau khi rotate refresh.
 */
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
) {
}
