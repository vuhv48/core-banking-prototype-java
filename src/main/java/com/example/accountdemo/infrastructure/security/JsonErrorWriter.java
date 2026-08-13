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
 * Ghi lỗi JSON thống nhất từ filter / entry point (trước khi vào controller).
 */
@Component
@RequiredArgsConstructor
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorStatus status)
            throws IOException {
        write(request, response, status, status.defaultMessage());
    }

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
