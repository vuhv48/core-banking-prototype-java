package com.example.accountdemo.api.auth.dto;

/**
 * Body đăng nhập (username + password).
 *
 * <p><b>Vì sao cần class này:</b> DTO tách khỏi entity User — API chỉ nhận credential cần thiết.
 */
public record LoginRequest(String username, String password) {
}
