package com.example.accountdemo.domain.exchange.event;

/**
 * Port — publish {@link TradeExecutedEvent}.
 *
 * <p>Hexagonal: domain nói "đã khớp, hãy thông báo"; infrastructure log / Kafka sau này.
 * <p>Không phải Aggregate / VO.
 */
public interface DomainEventPublisher {

    void publish(TradeExecutedEvent event);
}
