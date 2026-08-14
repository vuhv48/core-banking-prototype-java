package com.example.accountdemo.application;

import com.example.accountdemo.domain.exchange.order.model.Order;

public interface OwnershipChecker {

    void requireAccountAccess(String username, String accountId);

    void requireOrderAccess(String username, Order order);
}
