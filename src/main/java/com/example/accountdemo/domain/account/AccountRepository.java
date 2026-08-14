package com.example.accountdemo.domain.account;

import com.example.accountdemo.domain.account.model.Account;

/**
 * Port (Repository) — persistence của aggregate {@link Account}.
 *
 * <p><b>Vì sao cần:</b> domain khai báo "cần load/save Account" mà không phụ thuộc JPA.
 * Implement ở infrastructure. Không chứa business rule — rule nằm trong {@link Account}.
 */
public interface AccountRepository {

    /** Load ví theo id (null / throw tùy implement — Application tự xử lý không tìm thấy). */
    Account findById(String accountId);

    /** Lưu / cập nhật toàn bộ holdings sau deposit, reserve, settle… */
    void save(Account account);
}
