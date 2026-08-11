package com.example.accountdemo.domain.exchange;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object — output tạm của {@link OrderMatchingService#match}, không lưu DB.
 *
 * <p>Phân loại DDD:
 * <ul>
 *   <li>Không phải Aggregate — không có identity, không persist</li>
 *   <li>Không phải Value Object — chỉ đóng gói kết quả 1 lần gọi service, không mô tả khái niệm nghiệp vụ</li>
 * </ul>
 *
 * Chứa:
 * <ul>
 *   <li>{@code trades} — các {@link Trade} (VO) đã khớp</li>
 *   <li>{@code affectedOrders} — các {@link Order} (Aggregate) đã đổi status/filledQuantity, cần save</li>
 * </ul>
 */
public final class MatchResult {

    private final List<Trade> trades;
    private final List<Order> affectedOrders;

    public MatchResult(List<Trade> trades, List<Order> affectedOrders) {
        this.trades = List.copyOf(trades);
        this.affectedOrders = List.copyOf(affectedOrders);
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public List<Order> getAffectedOrders() {
        return affectedOrders;
    }
}
