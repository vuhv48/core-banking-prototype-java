package com.example.accountdemo.api.dto;

/**
 * Admin tạo user login.
 * {@code accountId} null/blank → tạo ví mới; có giá trị → gắn ví đã tồn tại (chưa bị user khác giữ).
 */
public record AdminCreateUserRequest(
        String username,
        String password,
        String email,
        String accountId
) {
}
