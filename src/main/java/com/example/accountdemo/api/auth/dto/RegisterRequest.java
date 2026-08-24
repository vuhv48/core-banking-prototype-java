package com.example.accountdemo.api.auth.dto;

/**
 * Body đăng ký tài khoản mới.
 */
public record RegisterRequest(
        String username,
        String password,
        String email
) {
}
