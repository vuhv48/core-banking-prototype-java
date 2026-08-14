package com.example.accountdemo.domain.exchange;

import lombok.Getter;

/**
 * Aggregate Root — lệnh giao dịch (mua/bán).
 * Tự bảo vệ rule: LIMIT phải có giá, không cancel khi đã FILLED, match đúng số lượng.
 * Chỉ @Getter — không @Setter/@Data để không bỏ qua match()/cancel().
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
     * Khôi phục Order từ persistence (không đi qua rule tạo lệnh mới).
     * Dùng khi load từ DB — giữ nguyên filledQuantity và status đã lưu.
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
            OrderStatus status
    ) {
        return reconstitute(
                orderId, accountId, side, orderType, tradingPair, quantity, price,
                filledQuantity, status, null, 0
        );
    }

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

    public void cancel() {
        if (status.isFinal()) {
            throw new IllegalStateException("Không thể hủy lệnh đã kết thúc: " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    public Quantity getRemainingQuantity() {
        return quantity.minus(filledQuantity);
    }
}
