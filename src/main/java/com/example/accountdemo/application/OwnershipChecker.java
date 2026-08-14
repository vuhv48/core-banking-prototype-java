package com.example.accountdemo.application;

import com.example.accountdemo.domain.exchange.order.model.Order;

/**
 * Hợp đồng kiểm tra quyền sở hữu account / order.
 * <p><b>Vì sao cần class này:</b> tách rule ownership khỏi từng use case,
 * để mọi API (đặt lệnh, hủy, xem tài khoản…) dùng chung một cổng kiểm tra.
 */
public interface OwnershipChecker {

    /**
     * Đảm bảo user được thao tác accountId; nếu không thì chặn request.
     */
    void requireAccountAccess(String username, String accountId);

    /**
     * Đảm bảo user sở hữu lệnh (qua account gắn lệnh); nếu không thì chặn request.
     */
    void requireOrderAccess(String username, Order order);
}
