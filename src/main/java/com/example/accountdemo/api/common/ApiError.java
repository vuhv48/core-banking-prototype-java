package com.example.accountdemo.api.common;

import java.time.Instant;
import java.util.Map;

/**
 * Chi tiết lỗi trong envelope API (timestamp, HTTP status, code, message, path, details).
 *
 * <p><b>Vì sao cần class này:</b> client đọc lỗi theo schema cố định; factory {@code of} gom
 * tạo lỗi từ {@link ErrorStatus} hoặc tham số thô.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> details
) {
    /** Tạo lỗi không có field-level details. */
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, Map.of());
    }

    /** Tạo lỗi kèm map chi tiết (vd validation từng field). */
    public static ApiError of(
            int status,
            String code,
            String message,
            String path,
            Map<String, String> details
    ) {
        return new ApiError(
                Instant.now(),
                status,
                code,
                message,
                path,
                details == null ? Map.of() : Map.copyOf(details)
        );
    }

    /** Tạo lỗi từ {@link ErrorStatus}; message trống thì dùng default. */
    public static ApiError of(ErrorStatus errorStatus, String message, String path) {
        String msg = (message != null && !message.isBlank())
                ? message
                : errorStatus.defaultMessage();
        return of(errorStatus.httpStatus(), errorStatus.code(), msg, path);
    }
}
