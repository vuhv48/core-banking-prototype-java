package com.example.accountdemo.api.common;

/**
 * Envelope JSON thống nhất: success + data hoặc error.
 *
 * <p><b>Vì sao cần class này:</b> mọi response (kể cả lỗi qua RestExceptionHandler) cùng shape —
 * client không phải đoán format theo từng endpoint.
 */
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    /** Response thành công kèm payload. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Response thất bại kèm {@link ApiError}. */
    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
