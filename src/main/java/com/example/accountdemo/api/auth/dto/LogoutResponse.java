package com.example.accountdemo.api.auth.dto;

/**
 * Response logout (thông báo ngắn).
 *
 * <p><b>Vì sao cần class này:</b> thống nhất shape JSON khi đăng xuất thành công.
 */
public record LogoutResponse(String message) {
}
