package com.example.accountdemo.api.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        List<String> permissions
) {
}
