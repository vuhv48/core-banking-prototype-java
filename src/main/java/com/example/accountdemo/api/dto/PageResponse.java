package com.example.accountdemo.api.dto;

import java.util.List;

/** Envelope phân trang cho API. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / (double) size);
        boolean hasNext = page + 1 < totalPages;
        return new PageResponse<>(content, page, size, totalElements, totalPages, hasNext);
    }
}
