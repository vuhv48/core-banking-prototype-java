package com.example.accountdemo.infrastructure.event;

import com.example.accountdemo.domain.exchange.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter Sprint 5 — chỉ log ra console.
 * Sau này Kafka/Outbox = thêm implementation khác của cùng port.
 */
@Slf4j
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(TradeExecutedEvent event) {
        log.info("Trade executed: {}", event);
    }
}
