package com.example.accountdemo.domain.exchange.order;

import com.example.accountdemo.domain.exchange.order.model.Order;

import java.util.List;

/**
 * Kết quả phân trang lệnh — không phụ thuộc Spring Data Page.
 */
public record OrderPage(
        List<Order> content,
        int page,
        int size,
        long totalElements
) {
    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / (double) size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
