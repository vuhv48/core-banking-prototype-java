package com.example.accountdemo.api.auth.dto;

/**
 * Response sau khi đăng ký thành công.
 */
public record RegisterResponse(
        String username,
        String accountId,
        String message
) {
}
