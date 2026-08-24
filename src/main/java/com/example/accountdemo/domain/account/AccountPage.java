package com.example.accountdemo.domain.account;

import com.example.accountdemo.domain.account.model.Account;

import java.util.List;

/**
 * Kết quả phân trang thuần domain — không phụ thuộc Spring Data Page.
 */
public record AccountPage(
        List<Account> content,
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
