package com.example.accountdemo.api.common;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> details
) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, Map.of());
    }

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

    public static ApiError of(ErrorStatus errorStatus, String message, String path) {
        String msg = (message != null && !message.isBlank())
                ? message
                : errorStatus.defaultMessage();
        return of(errorStatus.httpStatus(), errorStatus.code(), msg, path);
    }
}
