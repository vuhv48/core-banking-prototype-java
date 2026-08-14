package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.api.common.ApiError;
import com.example.accountdemo.api.common.ApiResponse;
import com.example.accountdemo.api.common.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ghi lỗi JSON thống nhất từ filter / entry point (trước controller).
 *
 * <p><b>Vì sao cần class này:</b> 401/403 từ security cùng shape {@link com.example.accountdemo.api.common.ApiResponse}.
 */
@Component
@RequiredArgsConstructor
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    /** Ghi response lỗi theo ErrorStatus (message mặc định). */
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorStatus status)
            throws IOException {
        write(request, response, status, status.defaultMessage());
    }

    /** Ghi response lỗi với message tùy chỉnh. */
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorStatus status,
            String message
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(status, message, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(error));
    }
}
