package com.example.accountdemo.infrastructure.event;

import com.example.accountdemo.domain.exchange.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter Sprint 5 — chỉ log ra console.
 * Sau này Kafka/Outbox = thêm implementation khác của cùng port.
 */
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(TradeExecutedEvent event) {
        log.info("Trade executed: {}", event);
    }
}
