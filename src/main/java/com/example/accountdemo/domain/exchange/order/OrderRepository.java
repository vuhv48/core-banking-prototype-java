package com.example.accountdemo.domain.exchange.order;

import com.example.accountdemo.domain.exchange.order.model.Order;

public interface OrderRepository {

    Order findById(String orderId);

    void save(Order order);
}
