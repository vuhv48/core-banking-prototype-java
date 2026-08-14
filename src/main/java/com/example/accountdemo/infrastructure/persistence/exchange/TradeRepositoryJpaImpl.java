package com.example.accountdemo.infrastructure.persistence.exchange;

import com.example.accountdemo.domain.exchange.trade.model.ExecutedTrade;
import com.example.accountdemo.domain.exchange.trade.TradeRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Adapter triển khai {@code TradeRepository} bằng Spring Data JPA.
 *
 * <p><b>Vì sao cần class này:</b> persist ExecutedTrade sau khớp lệnh qua port domain.
 */
@Repository
@RequiredArgsConstructor
public class TradeRepositoryJpaImpl implements TradeRepository {

    private final TradeJpaRepository tradeJpaRepository;

    /** Persist một ExecutedTrade vào bảng trades. */
    @Override
    @Transactional
    public void save(ExecutedTrade trade) {
        LocalDateTime now = LocalDateTime.now();
        TradeJpaEntity entity = new TradeJpaEntity();
        entity.setId(trade.getTradeId());
        entity.setBuyOrderId(trade.getBuyOrderId());
        entity.setSellOrderId(trade.getSellOrderId());
        entity.setBuyerAccountId(trade.getBuyerAccountId());
        entity.setSellerAccountId(trade.getSellerAccountId());
        entity.setBaseCurrency(trade.getTradingPair().getBaseCurrency());
        entity.setQuoteCurrency(trade.getTradingPair().getQuoteCurrency());
        entity.setQuantity(trade.getQuantity().getValue());
        entity.setPrice(trade.getPrice().getValue());
        entity.setDeleted(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        tradeJpaRepository.save(entity);
    }
}
