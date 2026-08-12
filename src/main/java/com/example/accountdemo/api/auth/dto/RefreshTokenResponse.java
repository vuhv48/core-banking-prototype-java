package com.example.accountdemo.api.auth.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {
}
