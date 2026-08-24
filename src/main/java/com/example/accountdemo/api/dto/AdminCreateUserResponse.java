package com.example.accountdemo.api.dto;

/** Response admin tạo user thành công. */
public record AdminCreateUserResponse(
        String username,
        String accountId,
        String message
) {
}
