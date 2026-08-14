package com.example.accountdemo.domain.exchange.order;

import com.example.accountdemo.domain.exchange.order.model.Order;

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
}
