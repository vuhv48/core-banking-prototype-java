package com.example.accountdemo.domain.exchange.order.model;

import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import lombok.Getter;

/**
 * Aggregate Root — một lệnh mua/bán trên sàn.
 *
 * <p><b>Vì sao cần class này:</b> mọi đổi trạng thái khớp/hủy và phần locked còn lại
 * phải đi qua đây để giữ invariant: không khớp quá remaining, lệnh final không match/cancel lại.
 * Application không {@code set} thẳng filled/status.
 *
 * <pre>
 * orderId               = ORD-BUY-001
 * accountId             = ACC-001
 * side                  = BUY
 * orderType             = LIMIT
 * tradingPair           = BTC/VND
 * quantity              = 1
 * price                 = 60_000_000
 * filledQuantity        = 0
 * status                = PENDING
 * lockedCurrency        = VND
 * lockedAmountRemaining = 60_000_000
 * </pre>
 *
 * Identity: {@code orderId}. Đổi state qua {@code match()}/{@code cancel()} — không {@code @Setter}.
 */
@Getter
public class Order {

    /** Id duy nhất của lệnh (vd UUID). */
    private String orderId;
    /** Ai đặt lệnh (vd ACC-001). */
    private String accountId;
    /** Mua hay bán: BUY / SELL. */
    private OrderSide side;
    /** Kiểu lệnh: LIMIT (có giá) / MARKET (khớp giá thị trường). */
    private OrderType orderType;
    /** Cặp giao dịch, vd BTC/VND. */
    private TradingPair tradingPair;
    /** Số lượng muốn mua/bán (tổng). */
    private Quantity quantity;
    /** Giá LIMIT (MARKET thì null). */
    private Price price;
    /** Số lượng đã khớp thành công (ban đầu = 0). */
    private Quantity filledQuantity;
    /** Trạng thái: PENDING → PARTIALLY_FILLED → FILLED (hoặc CANCELLED). */
    private OrderStatus status;
    /** Currency đang treo trên lệnh (VND khi BUY, BTC khi SELL). */
    private String lockedCurrency;
    /** Phần locked còn lại chưa settle/release. */
    private long lockedAmountRemaining;

    /**
     * Tạo lệnh mới (PENDING, filled = 0, chưa lock).
     * LIMIT bắt buộc có price; lock gắn sau qua {@link #initializeLock}.
     */
    public Order(
            String orderId,
            String accountId,
            OrderSide side,
            OrderType orderType,
            TradingPair tradingPair,
            Quantity quantity,
            Price price
    ) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId không được null hoặc rỗng");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId không được null hoặc rỗng");
        }
        if (side == null) {
            throw new IllegalArgumentException("side không được null");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("orderType không được null");
        }
        if (tradingPair == null) {
            throw new IllegalArgumentException("tradingPair không được null");
        }
        if (quantity == null || quantity.getValue() <= 0) {
            throw new IllegalArgumentException("quantity phải lớn hơn 0");
        }
        if (orderType == OrderType.LIMIT && price == null) {
            throw new IllegalArgumentException("LIMIT order phải có giá (price không được null)");
        }
        this.orderId = orderId;
        this.accountId = accountId;
        this.side = side;
        this.orderType = orderType;
        this.tradingPair = tradingPair;
        this.quantity = quantity;
        this.price = price;
        this.filledQuantity = new Quantity(0);
        this.status = OrderStatus.PENDING;
        this.lockedCurrency = null;
        this.lockedAmountRemaining = 0;
    }

    /**
     * Khôi phục Order đầy đủ từ DB — giữ filled, status và phần lock còn lại.
     * Dùng khi load để cancel/settle đúng số đã treo.
     */
    public static Order reconstitute(
            String orderId,
            String accountId,
            OrderSide side,
            OrderType orderType,
            TradingPair tradingPair,
            Quantity quantity,
            Price price,
            Quantity filledQuantity,
            OrderStatus status,
            String lockedCurrency,
            long lockedAmountRemaining
    ) {
        Order order = new Order(orderId, accountId, side, orderType, tradingPair, quantity, price);
        order.filledQuantity = filledQuantity != null ? filledQuantity : new Quantity(0);
        order.status = status != null ? status : OrderStatus.PENDING;
        order.lockedCurrency = lockedCurrency;
        order.lockedAmountRemaining = lockedAmountRemaining;
        return order;
    }

    /** Gán số đang treo sau khi Account.reserve thành công. */
    public void initializeLock(String currency, long amount) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("lockedCurrency không được rỗng");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("lockedAmount phải lớn hơn 0");
        }
        this.lockedCurrency = currency;
        this.lockedAmountRemaining = amount;
    }

    /** Giảm phần lock đã dùng khi settle (theo giá limit × qty hoặc qty base). */
    public void reduceLock(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount không được âm");
        }
        if (amount > lockedAmountRemaining) {
            throw new IllegalArgumentException("reduceLock vượt quá locked còn lại");
        }
        this.lockedAmountRemaining -= amount;
    }

    /**
     * Ghi nhận một lần khớp: tăng filled, đổi PENDING → PARTIALLY_FILLED / FILLED.
     * Không trừ ví — Application settle sau theo Trade.
     */
    public void match(Quantity executedQuantity) {
        if (executedQuantity == null || executedQuantity.getValue() <= 0) {
            throw new IllegalArgumentException("executedQuantity phải lớn hơn 0");
        }
        if (status.isFinal()) {
            throw new IllegalStateException("Không thể khớp lệnh đã kết thúc: " + status);
        }

        Quantity remaining = getRemainingQuantity();
        if (!remaining.isGreaterThanOrEqual(executedQuantity)) {
            throw new IllegalArgumentException("Số lượng khớp vượt quá số lượng còn lại");
        }

        filledQuantity = filledQuantity.plus(executedQuantity);
        if (getRemainingQuantity().isZero()) {
            status = OrderStatus.FILLED;
        } else {
            status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    /** Hủy lệnh chưa kết thúc — phần lock còn lại Application sẽ release về ví. */
    public void cancel() {
        if (status.isFinal()) {
            throw new IllegalStateException("Không thể hủy lệnh đã kết thúc: " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    /** Số lượng còn lại chưa khớp = quantity − filledQuantity. */
    public Quantity getRemainingQuantity() {
        return quantity.minus(filledQuantity);
    }
}
