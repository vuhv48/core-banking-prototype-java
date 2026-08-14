package com.example.accountdemo.domain.exchange.event;

/**
 * Port — publish {@link TradeExecutedEvent}.
 *
 * <p><b>Vì sao cần:</b> hexagonal — domain nói "đã khớp, hãy thông báo";
 * infrastructure log / Kafka sau này. Không phải Aggregate / VO.
 */
public interface DomainEventPublisher {

    /** Đẩy sự kiện vừa khớp xong ra ngoài (log, message bus…). */
    void publish(TradeExecutedEvent event);
}
