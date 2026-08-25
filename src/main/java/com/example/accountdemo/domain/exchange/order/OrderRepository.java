package com.example.accountdemo.domain.exchange.order;

import com.example.accountdemo.domain.exchange.order.model.Order;

import java.util.List;

/**
 * Port (Repository) — persistence của aggregate {@link Order}.
 *
 * <p><b>Vì sao cần:</b> domain khai báo load/save lệnh; JPA implement ở infrastructure.
 * Không chứa rule khớp lệnh.
 */
public interface OrderRepository {

    /** Load lệnh theo id (kèm filled/status/lock còn lại). */
    Order findById(String orderId);

    /** Lưu lệnh sau place / match / cancel. */
    void save(Order order);

    List<Order> findByAccountId(String accountId);

    /**
     * Phân trang. {@code accountId}/{@code orderId} null = không lọc;
     * có giá trị thì khớp <em>chứa</em> chuỗi (không cần đúng cả ID).
     */
    OrderPage findPage(int page, int size, String accountId, String orderId);
}
