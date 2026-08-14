package com.example.accountdemo.api.dto;

/**
 * Response sau đặt lệnh: orderId + status hiện tại.
 *
 * <p><b>Vì sao cần class này:</b> client chỉ cần xác nhận lệnh đã tạo, không cần full OrderResponse.
 */
public record PlaceOrderResponse(String orderId, String status) {
}
