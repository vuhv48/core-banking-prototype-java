package com.example.accountdemo.domain.exchange;

/**
 * Value Object — mô tả một lần khớp thành công giữa lệnh mua và lệnh bán.
 *
 * <p>Phân loại DDD:
 * <ul>
 *   <li>Immutable, không có identity riêng (so sánh theo giá trị)</li>
 *   <li>Không phải Aggregate — không lưu DB trực tiếp; sinh ra trong {@link OrderMatchingService}</li>
 * </ul>
 */
public final class Trade {

    private final String buyOrderId;
    private final String sellOrderId;
    private final Quantity matchedQuantity;
    private final Price matchedPrice;

    public Trade(
            String buyOrderId,
            String sellOrderId,
            Quantity matchedQuantity,
            Price matchedPrice
    ) {
        if (buyOrderId == null || buyOrderId.isBlank()) {
            throw new IllegalArgumentException("buyOrderId không được null hoặc rỗng");
        }
        if (sellOrderId == null || sellOrderId.isBlank()) {
            throw new IllegalArgumentException("sellOrderId không được null hoặc rỗng");
        }
        if (matchedQuantity == null || matchedQuantity.getValue() <= 0) {
            throw new IllegalArgumentException("matchedQuantity phải lớn hơn 0");
        }
        if (matchedPrice == null) {
            throw new IllegalArgumentException("matchedPrice không được null");
        }
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.matchedQuantity = matchedQuantity;
        this.matchedPrice = matchedPrice;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public Quantity getMatchedQuantity() {
        return matchedQuantity;
    }

    public Price getMatchedPrice() {
        return matchedPrice;
    }
}
