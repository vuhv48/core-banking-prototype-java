package com.example.accountdemo.domain.exchange.matching;

import java.util.List;
import lombok.Getter;
import com.example.accountdemo.domain.exchange.order.model.Order;

/**
 * Result object — output một lần gọi {@code match} (không phải bản ghi DB).
 *
 * <p><b>Vì sao cần class này:</b> gom danh sách Trade đã khớp + mọi Order bị đổi status
 * để Application biết save gì / settle gì — matching không tự persist.
 *
 * <pre>
 * trades = [
 *   { buyOrderId=ORD-BUY-001, sellOrderId=ORD-SELL-001, qty=1, price=60_000_000 }
 * ]
 * affectedOrders = [
 *   ORD-BUY-001  FILLED            filled=1
 *   ORD-SELL-001 PARTIALLY_FILLED  filled=1 remaining=1
 * ]
 * </pre>
 */
@Getter
public final class MatchResult {

    /** Các lần khớp thành công trong lần gọi match(). */
    private final List<Trade> trades;
    /** Mọi Order bị đổi (lệnh mới + đối ứng) — Application cần save. */
    private final List<Order> affectedOrders;

    /** Đóng băng hai list — ngoài không sửa kết quả matching. */
    public MatchResult(List<Trade> trades, List<Order> affectedOrders) {
        this.trades = List.copyOf(trades);
        this.affectedOrders = List.copyOf(affectedOrders);
    }
}
