package com.example.accountdemo.domain.exchange.order;

import com.example.accountdemo.domain.exchange.order.model.Order;

/**
 * Port (Repository) — persistence của aggregate {@link Order}.
 *
 * <p>Domain khai báo load/save; JPA implement ở infrastructure. Không chứa rule khớp lệnh.
 */
public interface OrderRepository {

    Order findById(String orderId);

    void save(Order order);
}
