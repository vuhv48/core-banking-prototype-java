package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Money;
import com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.matching.Trade;
import com.example.accountdemo.domain.exchange.trade.TradeRepository;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * Tất toán ví hai bên khi khớp lệnh và ghi bản ghi ExecutedTrade.
 * <p><b>Vì sao cần class này:</b> matching chỉ tạo Trade trên sổ lệnh; cần bước riêng
 * để chuyển locked → số dư thực và lưu lịch sử giao dịch — tách khỏi PlaceOrder
 * để reuse khi có thêm luồng khớp sau này.
 */
@Service
@RequiredArgsConstructor
public class TradeSettlementService {

    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;

    /**
     * Settle một Trade: buyer/seller consume locked, credit tài sản nhận được, lưu trades.
     */
    public void settle(Trade trade, TradingPair tradingPair, Map<String, Order> ordersById) {
        Order buyOrder = requireOrder(ordersById, trade.getBuyOrderId());
        Order sellOrder = requireOrder(ordersById, trade.getSellOrderId());

        if (buyOrder.getAccountId().equals(sellOrder.getAccountId())) {
            throw new DomainException(ErrorStatus.ORDER_INVALID, "Không hỗ trợ tự khớp cùng một account");
        }

        long qty = trade.getMatchedQuantity().getValue();
        long price = trade.getMatchedPrice().getValue();
        long notional = qty * price;

        String quote = tradingPair.getQuoteCurrency();
        String base = tradingPair.getBaseCurrency();

        Account buyer = requireAccount(buyOrder.getAccountId());
        Account seller = requireAccount(sellOrder.getAccountId());

        // Buyer: chi VND đã treo, nhận base
        buyer.consumeLocked(new Money(notional, quote));
        long buyerLockForQty = lockPortionForFill(buyOrder, qty);
        long buyerExcess = buyerLockForQty - notional;
        if (buyerExcess > 0) {
            buyer.release(new Money(buyerExcess, quote));
        }
        buyOrder.reduceLock(buyerLockForQty);
        buyer.credit(new Money(qty, base));

        // Seller: chi base đã treo, nhận quote
        seller.consumeLocked(new Money(qty, base));
        sellOrder.reduceLock(qty);
        seller.credit(new Money(notional, quote));

        accountRepository.save(buyer);
        accountRepository.save(seller);

        String tradeId = UUID.randomUUID().toString();
        tradeRepository.save(new ExecutedTrade(
                tradeId,
                buyOrder.getOrderId(),
                sellOrder.getOrderId(),
                buyer.getAccountId(),
                seller.getAccountId(),
                tradingPair,
                trade.getMatchedQuantity(),
                trade.getMatchedPrice()
        ));
    }

    /**
     * Phần lock gắn với qty vừa khớp:
     * BUY LIMIT → qty × limitPrice; SELL → qty (base).
     */
    private long lockPortionForFill(Order order, long qty) {
        if (order.getSide() == OrderSide.BUY) {
            if (order.getPrice() == null) {
                throw new DomainException(ErrorStatus.ORDER_INVALID, "BUY thiếu giá limit để settle");
            }
            return qty * order.getPrice().getValue();
        }
        return qty;
    }

    private Order requireOrder(Map<String, Order> ordersById, String orderId) {
        Order order = ordersById.get(orderId);
        if (order == null) {
            throw new DomainException(ErrorStatus.ORDER_NOT_FOUND, "Không tìm thấy lệnh: " + orderId);
        }
        return order;
    }

    private Account requireAccount(String accountId) {
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND, "Không tìm thấy tài khoản: " + accountId);
        }
        return account;
    }
}
