package com.example.accountdemo.domain.exchange;

public interface OrderRepository {

    Order findById(String orderId);

    void save(Order order);
}
