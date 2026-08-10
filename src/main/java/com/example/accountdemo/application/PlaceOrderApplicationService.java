package com.example.accountdemo.application;

import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.OrderRepository;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderType;
import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.TradingPair;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application Service — điều phối use case đặt lệnh.
 * KHÔNG chứa business rule; chỉ gọi domain + repository.
 * Sprint 3: tạo order + đưa vào OrderBook đã mở. Chưa khớp lệnh (Sprint 4).
 *
 * Luồng chung (giống deposit Account):
 * load từ DB → domain xử lý trên object (memory tạm) → save lại DB.
 */
@Service
public class PlaceOrderApplicationService {

    /** Port lưu/tải từng lệnh (bảng orders). */
    private final OrderRepository orderRepository;

    /** Port lưu/tải sổ lệnh theo cặp (ghép order_books + orders). */
    private final OrderBookRepository orderBookRepository;

    public PlaceOrderApplicationService(
            OrderRepository orderRepository,
            OrderBookRepository orderBookRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderBookRepository = orderBookRepository;
    }

    /**
     * Use case: đặt lệnh mua/bán.
     * Chỉ điều phối — business rule nằm trong Order / OrderBook.
     */
    public String placeOrder(
            String accountId,
            OrderSide side,
            OrderType orderType,
            TradingPair tradingPair,
            Quantity quantity,
            Price price
    ) {
        // 1) Tạo id mới cho lệnh (chưa lưu DB).
        String orderId = UUID.randomUUID().toString();

        // 2) Tạo Aggregate Order trong memory.
        //    Constructor tự validate (vd LIMIT phải có price) — đây là business rule của Order.
        Order order = new Order(orderId, accountId, side, orderType, tradingPair, quantity, price);

        // 3) Load sổ lệnh của cặp từ DB → object OrderBook trong memory (bàn làm việc tạm).
        //    Bên trong repo: đọc order_books (cặp đã mở?) + orders (các lệnh hiện có) rồi ghép lại.
        OrderBook orderBook = orderBookRepository.findByTradingPair(tradingPair);

        // 4) Sổ do admin/seed mở trước. Chưa có dòng trong order_books → reject.
        //    Không auto-create OrderBook ở đây.
        if (orderBook == null) {
            throw new IllegalArgumentException("Cặp giao dịch chưa được mở: " + tradingPair);
        }

        // 5) Domain: gắn lệnh vào sổ (BUY → buyOrders / SELL → sellOrders, sort giá).
        //    Chỉ đổi object trong memory — chuẩn bị cho Sprint 4 match.
        orderBook.addOrder(order);

        // 6) Persistence: ghi lệnh mới xuống bảng orders.
        //    Không cần save(orderBook): metadata sổ không đổi; lệnh mới đã save ở trên.
        //    Sprint 4 (match nhiều lệnh) có thể save(orderBook) một lần cho cả sổ.
        orderRepository.save(order);

        return orderId;
    }
}
