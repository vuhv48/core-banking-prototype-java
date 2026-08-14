package com.example.accountdemo.domain.exchange.event;

/**
 * Port (interface) — publish domain event; implement ở infrastructure (log, Kafka…).
 *
 * <p>Hexagonal: domain khai báo "cần publish", không biết cách gửi cụ thể.
 */
public interface DomainEventPublisher {

    void publish(TradeExecutedEvent event);
}
