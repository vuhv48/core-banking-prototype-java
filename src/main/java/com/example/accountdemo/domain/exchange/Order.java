package com.example.accountdemo.domain.exchange;

/**
 * Aggregate Root — lệnh giao dịch (mua/bán).
 * Tự bảo vệ rule: LIMIT phải có giá, không cancel khi đã FILLED, match đúng số lượng.
 */
public class Order {

    private String orderId;
    private String accountId;
    private OrderSide side;
    private OrderType orderType;
    private TradingPair tradingPair;
    private Quantity quantity;
    private Price price;
    private Quantity filledQuantity;
    private OrderStatus status;

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

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public TradingPair getTradingPair() {
        return tradingPair;
    }

    public Price getPrice() {
        return price;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
