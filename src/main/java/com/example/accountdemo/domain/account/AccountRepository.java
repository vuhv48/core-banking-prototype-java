package com.example.accountdemo.domain.account;

import com.example.accountdemo.domain.account.model.Account;

import java.util.List;

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

    /** Tất cả account chưa soft-delete (dùng nội bộ / test). */
    List<Account> findAll();

    /**
     * Phân trang account chưa soft-delete.
     * @param page trang 0-based
     * @param size số phần tử mỗi trang (&gt; 0)
     */
    AccountPage findPage(int page, int size);
}
